package com.slatto.domain.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileUpdateRequest;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectFileRepository;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.global.exception.BaseException;
import com.slatto.global.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 파일 이름의 확장자를 누가 정하는지 고정한다.
 *
 * <p>예전에는 사용자가 적은 이름에서 확장자를 뽑아 Content-Type 과 대조했다. 그래서 png 를
 * 올리면서 이름을 "무드보드" 라고만 적으면 파일 형식이 잘못됐다며 거부했다. 이름은 화면에
 * 보일 값일 뿐이므로, 확장자는 올라온 파일에서 가져와 서버가 붙인다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectFileNameExtensionTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Long PROJECT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long FILE_ID = 100L;

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private ProjectAccessValidator projectAccessValidator;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectFileService projectFileService;

    private Users uploader;
    private Project project;

    @BeforeEach
    void setUp() {
        uploader = Users.createSocialUser("uploader@slatto.com", "업로더", null, SocialType.GOOGLE, "social-1");
        ReflectionTestUtils.setField(uploader, "id", USER_ID);

        project = Project.create(
            uploader,
            "연애혁명",
            CategoryName.FILM_DRAMA,
            LengthType.SHORT_FORM,
            "웹드라마 촬영",
            LocalDate.now().plusDays(30),
            "스튜디오 X",
            Kind.EXTERNAL
        );
        ProjectMember member = ProjectMember.createAdmin(project, uploader);

        given(projectAccessValidator.getProjectOrThrow(PROJECT_ID)).willReturn(project);
        given(projectAccessValidator.getCurrentMemberOrThrow(PROJECT_ID, USER_ID)).willReturn(member);
        given(projectMemberRepository.findAllActiveMembersByProjectId(anyLong())).willReturn(List.of(member));
        given(projectFileRepository.save(any(ProjectFile.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("확장자 없이 이름만 적어도 올린 파일의 확장자가 붙는다")
    void upload_appendsExtensionFromUploadedFile() {
        ProjectFileResponse response = upload("무드보드", pngFile());

        assertThat(response.getFileName()).isEqualTo("무드보드.png");
    }

    @Test
    @DisplayName("이름에 이미 같은 확장자가 있으면 그대로 둔다")
    void upload_keepsNameWhenExtensionAlreadyMatches() {
        ProjectFileResponse response = upload("무드보드.png", pngFile());

        assertThat(response.getFileName()).isEqualTo("무드보드.png");
    }

    @Test
    @DisplayName("이름에 다른 확장자를 적으면 올린 파일 쪽을 따른다")
    void upload_replacesMismatchedExtension() {
        ProjectFileResponse response = upload("무드보드.pdf", pngFile());

        assertThat(response.getFileName()).isEqualTo("무드보드.png");
    }

    @Test
    @DisplayName("확장자가 아닌 점은 이름의 일부로 남는다")
    void upload_keepsDotsThatAreNotExtensions() {
        ProjectFileResponse response = upload("콘티 v1.2", pngFile());

        assertThat(response.getFileName()).isEqualTo("콘티 v1.2.png");
    }

    @Test
    @DisplayName("이름이 최대 길이여도 확장자를 붙인 결과가 컬럼 길이를 넘지 않는다")
    void upload_keepsNameWithinColumnLength() {
        ProjectFileResponse response = upload("가".repeat(255), pngFile());

        assertThat(response.getFileName()).hasSize(255).endsWith(".png");
    }

    @Test
    @DisplayName("올린 파일 자체의 확장자와 Content-Type 이 어긋나면 거부한다")
    void upload_rejectsFileWhoseExtensionDoesNotMatchContentType() {
        MockMultipartFile disguised = new MockMultipartFile(
            "file", "실행파일.exe", "image/png", "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> upload("무드보드", disguised))
            .isInstanceOf(BaseException.class)
            .extracting("errorCode")
            .isEqualTo(ProjectErrorCode.PROJECT_FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("저장 키는 올린 파일의 확장자로 만든다")
    void upload_buildsStorageKeyFromUploadedFile() {
        upload("무드보드", pngFile());

        ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(storageService).upload(any(), storageKey.capture());

        assertThat(storageKey.getValue())
            .startsWith("projects/%d/files/".formatted(PROJECT_ID))
            .endsWith(".png");
    }

    @Test
    @DisplayName("이름을 바꿔도 올릴 때의 확장자가 유지된다")
    void update_keepsOriginalExtension() {
        givenStoredFile("무드보드.png", "image/png");

        ProjectFileResponse response = projectFileService.updateProjectFile(
            PROJECT_ID, FILE_ID, USER_ID, updateRequest("최종 시안")
        );

        assertThat(response.getFileName()).isEqualTo("최종 시안.png");
    }

    @Test
    @DisplayName("이름을 바꾸며 다른 확장자를 적어도 올릴 때의 확장자를 따른다")
    void update_ignoresMismatchedExtension() {
        givenStoredFile("무드보드.png", "image/png");

        ProjectFileResponse response = projectFileService.updateProjectFile(
            PROJECT_ID, FILE_ID, USER_ID, updateRequest("최종 시안.pdf")
        );

        assertThat(response.getFileName()).isEqualTo("최종 시안.png");
    }

    private ProjectFileResponse upload(String fileName, MockMultipartFile file) {
        return projectFileService.uploadProjectFile(PROJECT_ID, USER_ID, uploadRequest(fileName), file);
    }

    private void givenStoredFile(String fileName, String contentType) {
        ProjectFile stored = ProjectFile.create(
            project, uploader, fileName, contentType, 1024L, "설명", false, "projects/1/files/key.png"
        );
        given(projectFileRepository.findActiveFileByProjectIdAndFileId(PROJECT_ID, FILE_ID))
            .willReturn(Optional.of(stored));
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "IMG_1234.PNG", "image/png", "png-bytes".getBytes());
    }

    private ProjectFileUploadRequest uploadRequest(String fileName) {
        return read("""
            {"fileName":%s,"description":"설명","isFinal":false}
            """.formatted(quote(fileName)), ProjectFileUploadRequest.class);
    }

    private ProjectFileUpdateRequest updateRequest(String fileName) {
        return read("""
            {"fileName":%s}
            """.formatted(quote(fileName)), ProjectFileUpdateRequest.class);
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
