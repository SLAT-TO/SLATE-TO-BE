package com.slatto.domain.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.notification.service.ActivityMessageFactory;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.domain.project.converter.ProjectConverter;
import com.slatto.domain.project.dto.ProjectUpdateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectUserRole;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.project.repository.ProjectUserRoleRepository;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserPortfolioRoleRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.service.PortfolioService;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import com.slatto.global.exception.BaseException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
    ProjectService.class,
    ProjectConverter.class,
    ProjectAccessValidator.class,
    PortfolioService.class,
    ActivityLogService.class,
    ActivityMessageFactory.class,
    YoutubeUrlParser.class,
    VideoRepository.class
})
class ProjectCompletionPortfolioTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectUserRoleRepository projectUserRoleRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private UserPortfolioRoleRepository userPortfolioRoleRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private NotificationService notificationService;

    private Users owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = saveUser("owner@slatto.com", "오너", "social-owner");
        project = projectRepository.save(Project.create(
            owner,
            "연애혁명",
            CategoryName.FILM_DRAMA,
            LengthType.SHORT_FORM,
            "웹드라마 촬영",
            LocalDate.now().plusDays(30),
            "스튜디오 X",
            Kind.EXTERNAL
        ));
        ProjectMember ownerMember = projectMemberRepository.save(ProjectMember.createAdmin(project, owner));
        projectUserRoleRepository.save(ProjectUserRole.create(ownerMember, RoleName.DIRECTOR));
        entityManager.flush();
    }

    @Test
    @DisplayName("완료로 바꾸면 참여 멤버 전원의 포트폴리오가 프로젝트 정보와 역할로 생성된다")
    void completeProject_createsPortfolioForEveryMember() {
        Users editor = saveUser("editor@slatto.com", "에디터", "social-editor");
        ProjectMember editorMember = projectMemberRepository.save(ProjectMember.createMember(project, editor));
        projectUserRoleRepository.save(ProjectUserRole.create(editorMember, RoleName.EDITOR));
        entityManager.flush();

        projectService.updateProject(project.getId(), owner.getId(), completeRequest());

        List<UserPortfolio> portfolios = userPortfolioRepository.findAll();
        assertThat(portfolios).hasSize(2);
        assertThat(portfolios).allSatisfy(portfolio -> {
            assertThat(portfolio.getTitle()).isEqualTo("연애혁명");
            assertThat(portfolio.getType()).isEqualTo(CategoryName.FILM_DRAMA);
            assertThat(portfolio.getKind()).isEqualTo(Kind.EXTERNAL);
            assertThat(portfolio.getClientName()).isEqualTo("스튜디오 X");
            assertThat(portfolio.getStartDate()).isEqualTo(project.getStartDate());
            assertThat(portfolio.getEndDate()).isEqualTo(project.getEndDate());
        });
        assertThat(roleNamesOf(editor)).containsExactly(RoleName.EDITOR);
        assertThat(roleNamesOf(owner)).containsExactly(RoleName.DIRECTOR);
    }

    @Test
    @DisplayName("나간 멤버와 탈퇴한 유저는 포트폴리오를 받지 않는다")
    void completeProject_excludesLeftMemberAndWithdrawnUser() {
        Users leftUser = saveUser("left@slatto.com", "나간사람", "social-left");
        ProjectMember leftMember = projectMemberRepository.save(ProjectMember.createMember(project, leftUser));
        leftMember.leave();

        Users withdrawnUser = saveUser("withdrawn@slatto.com", "탈퇴자", "social-withdrawn");
        projectMemberRepository.save(ProjectMember.createMember(project, withdrawnUser));
        withdrawnUser.withdraw(java.time.LocalDateTime.now());
        entityManager.flush();

        projectService.updateProject(project.getId(), owner.getId(), completeRequest());

        assertThat(userPortfolioRepository.findAll())
            .extracting(portfolio -> portfolio.getUser().getId())
            .containsExactly(owner.getId());
    }

    @Test
    @DisplayName("역할이 지정되지 않은 멤버도 역할 없이 포트폴리오를 받는다")
    void completeProject_createsPortfolioWithoutRoles() {
        Users helper = saveUser("helper@slatto.com", "도우미", "social-helper");
        projectMemberRepository.save(ProjectMember.createMember(project, helper));
        entityManager.flush();

        projectService.updateProject(project.getId(), owner.getId(), completeRequest());

        assertThat(userPortfolioRepository.findAll()).hasSize(2);
        assertThat(roleNamesOf(helper)).isEmpty();
    }

    @Test
    @DisplayName("완료된 프로젝트는 다른 진행 단계로 되돌릴 수 없다")
    void updateProject_afterCompletion_cannotChangeStatus() {
        projectService.updateProject(project.getId(), owner.getId(), completeRequest());
        entityManager.flush();

        assertThatThrownBy(() ->
            projectService.updateProject(project.getId(), owner.getId(), statusRequest("EDITING"))
        ).isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("개인/외주 구분이 없으면 완료로 바꿀 수 없다")
    void completeProject_withoutKind_throws() {
        Project noKindProject = projectRepository.save(Project.create(
            owner,
            "종류 없는 프로젝트",
            CategoryName.FILM_DRAMA,
            LengthType.SHORT_FORM,
            "설명",
            LocalDate.now().plusDays(10),
            null,
            null
        ));
        projectMemberRepository.save(ProjectMember.createAdmin(noKindProject, owner));
        entityManager.flush();

        assertThatThrownBy(() ->
            projectService.updateProject(noKindProject.getId(), owner.getId(), completeRequestWithoutKind())
        ).isInstanceOf(BaseException.class);

        assertThat(userPortfolioRepository.findAll()).isEmpty();
        assertThat(projectRepository.findById(noKindProject.getId()).orElseThrow().getStatus())
            .isNotEqualTo(ProjectStatus.COMPLETED);
    }

    private List<RoleName> roleNamesOf(Users user) {
        return userPortfolioRoleRepository.findAll()
            .stream()
            .filter(role -> role.getUser().getId().equals(user.getId()))
            .map(UserPortfolioRole::getRoleName)
            .toList();
    }

    private Users saveUser(String email, String nickname, String socialId) {
        return userRepository.save(
            Users.createSocialUser(email, nickname, null, SocialType.GOOGLE, socialId)
        );
    }

    private ProjectUpdateRequest completeRequest() {
        return request("""
            {"title":"연애혁명","type":"FILM_DRAMA","lengthType":"SHORT_FORM","description":"웹드라마 촬영",
             "endDate":"%s","clientName":"스튜디오 X","kind":"EXTERNAL","status":"COMPLETED"}
            """.formatted(LocalDate.now().plusDays(30)));
    }

    private ProjectUpdateRequest completeRequestWithoutKind() {
        return request("""
            {"title":"종류 없는 프로젝트","type":"FILM_DRAMA","lengthType":"SHORT_FORM","description":"설명",
             "endDate":"%s","status":"COMPLETED"}
            """.formatted(LocalDate.now().plusDays(10)));
    }

    private ProjectUpdateRequest statusRequest(String status) {
        return request("""
            {"title":"연애혁명","type":"FILM_DRAMA","lengthType":"SHORT_FORM","description":"웹드라마 촬영",
             "endDate":"%s","clientName":"스튜디오 X","kind":"EXTERNAL","status":"%s"}
            """.formatted(LocalDate.now().plusDays(30), status));
    }

    private ProjectUpdateRequest request(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ProjectUpdateRequest.class);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
