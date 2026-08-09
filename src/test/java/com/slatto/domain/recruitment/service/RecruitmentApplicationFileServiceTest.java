package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.entity.RecruitmentApplicationFile;
import com.slatto.domain.recruitment.exception.RecruitmentErrorCode;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationFileRepository;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentApplicationFileServiceTest {

    private static final Long WRITER_ID = 1L;
    private static final Long APPLICANT_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long RECRUITMENT_ID = 10L;
    private static final Long APPLICATION_ID = 100L;

    @Mock
    private RecruitmentApplicationFileRepository recruitmentApplicationFileRepository;

    @Mock
    private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private RecruitmentApplicationFileService recruitmentApplicationFileService;

    @Test
    @DisplayName("요청한 파일 중 하나라도 연결할 수 없으면 지원 전체를 실패시킨다")
    void rejectsWhenAnyFileIsNotLinkable() {
        // 두 개를 요청했는데 한 개만 통과했다면 나머지는 남의 파일이거나 이미 쓰인 파일이다.
        when(recruitmentApplicationFileRepository.findLinkableFiles(any(), anyLong(), anyLong()))
            .thenReturn(List.of(file(1L)));

        assertThatThrownBy(() -> recruitmentApplicationFileService.linkFilesToApplication(
            APPLICANT_ID, RECRUITMENT_ID, application(), List.of(1L, 2L)
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(RecruitmentErrorCode.APPLICATION_FILE_NOT_LINKABLE);
    }

    @Test
    @DisplayName("첨부 파일이 10개를 넘으면 거부한다")
    void rejectsMoreThanTenFiles() {
        List<Long> elevenFileIds = IntStream.rangeClosed(1, 11).mapToObj(Long::valueOf).toList();

        assertThatThrownBy(() -> recruitmentApplicationFileService.linkFilesToApplication(
            APPLICANT_ID, RECRUITMENT_ID, application(), elevenFileIds
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(RecruitmentErrorCode.APPLICATION_FILE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("같은 파일 id 를 중복으로 보내도 한 건으로 처리한다")
    void deduplicatesRequestedFileIds() {
        RecruitmentApplication application = application();
        when(recruitmentApplicationFileRepository.findLinkableFiles(any(), anyLong(), anyLong()))
            .thenReturn(List.of(file(1L)));

        List<RecruitmentApplicationFile> linked = recruitmentApplicationFileService.linkFilesToApplication(
            APPLICANT_ID, RECRUITMENT_ID, application, List.of(1L, 1L)
        );

        assertThat(linked).hasSize(1);
        assertThat(linked.get(0).getApplication()).isSameAs(application);
    }

    @Test
    @DisplayName("첨부 파일이 없으면 조회 없이 빈 목록을 돌려준다")
    void skipsQueryWhenNoFileRequested() {
        assertThat(recruitmentApplicationFileService.linkFilesToApplication(
            APPLICANT_ID, RECRUITMENT_ID, application(), null
        )).isEmpty();
    }

    @Test
    @DisplayName("공고 작성자도 지원자도 아니면 첨부 파일을 받을 수 없다")
    void deniesDownloadToStranger() {
        when(recruitmentRepository.findByIdAndDeletedAtIsNull(RECRUITMENT_ID))
            .thenReturn(Optional.of(recruitment()));
        when(recruitmentApplicationRepository
            .findByIdAndRecruitmentIdAndDeletedAtIsNull(APPLICATION_ID, RECRUITMENT_ID))
            .thenReturn(Optional.of(application()));

        assertThatThrownBy(() -> recruitmentApplicationFileService.downloadApplicationFile(
            STRANGER_ID, RECRUITMENT_ID, APPLICATION_ID, 1L
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    private Recruitment recruitment() {
        Recruitment recruitment = Recruitment.create(
            user(WRITER_ID), "공고 제목입니다", null, null, null, null, null, null, "010-0000-0000", "설명", null
        );
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);

        return recruitment;
    }

    private RecruitmentApplication application() {
        RecruitmentApplication application = RecruitmentApplication.create(
            user(APPLICANT_ID), recruitment(), "지원합니다", null
        );
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

        return application;
    }

    private RecruitmentApplicationFile file(Long id) {
        RecruitmentApplicationFile file = RecruitmentApplicationFile.create(
            recruitment(), user(APPLICANT_ID), "포트폴리오.pdf", "application/pdf", 1024L, "key-%d".formatted(id)
        );
        ReflectionTestUtils.setField(file, "id", id);

        return file;
    }

    private Users user(Long id) {
        Users user = Users.createSocialUser(
            "user%d@example.com".formatted(id), "사용자", null, SocialType.GOOGLE, "google-%d".formatted(id)
        );
        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}
