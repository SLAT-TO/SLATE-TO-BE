package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.entity.ProjectActivityRead;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.model.ActivityActor;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecentActivityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ProjectActivityReadRepository projectActivityReadRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 로그인한_프로젝트_멤버는_최근활동_목록을_새_활동_상태와_함께_조회한다() throws Exception {
        // JWT 인증, 프로젝트 멤버 권한, 목록 응답 형식, 초기 isRead=false를 한 요청 흐름으로 검증한다.
        Fixture fixture = createFixture();
        ActivityLog activity = saveActivity(fixture.project(), fixture.chaTaehoon(), "차태훈님이 새 공지를 등록했어요.");
        entityManager.flush();

        mockMvc.perform(get(activitiesUrl(fixture.project().getId()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon()))
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("COMMON200"))
            .andExpect(jsonPath("$.result.items[0].activityId").value(activity.getId()))
            .andExpect(jsonPath("$.result.items[0].content").value("차태훈님이 새 공지를 등록했어요."))
            .andExpect(jsonPath("$.result.items[0].isRead").value(false))
            .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void 인증_토큰이_없으면_최근활동_API에_접근할_수_없다() throws Exception {
        // 최근활동은 프로젝트 내부 정보이므로 SecurityFilterChain이 401 JSON 응답으로 차단해야 한다.
        Fixture fixture = createFixture();

        mockMvc.perform(get(activitiesUrl(fixture.project().getId())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("COMMON401"));
    }

    @Test
    void 프로젝트_비참여자는_유효한_JWT가_있어도_최근활동을_조회할_수_없다() throws Exception {
        // 인증과 프로젝트 접근 권한은 별개이므로, 로그인한 외부 사용자는 403이어야 한다.
        Fixture fixture = createFixture();
        Users green = saveUser("green@example.com", "그린", "google-green");

        mockMvc.perform(get(activitiesUrl(fixture.project().getId()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(green)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT403"));
    }

    @Test
    void 활동을_개별_확인하면_선택한_활동만_새_활동_표시가_해제된다() throws Exception {
        // 카드 클릭 직후 호출되는 read API가 대상 활동 하나만 읽음으로 만들고, 다른 활동의 빨간 점은 남겨야 한다.
        Fixture fixture = createFixture();
        ActivityLog first = saveActivity(fixture.project(), fixture.chaTaehoon(), "차태훈님이 파일을 등록했어요.");
        ActivityLog selected = saveActivity(fixture.project(), fixture.chaTaehoon(), "차태훈님이 공지를 등록했어요.");
        entityManager.flush();

        mockMvc.perform(patch(activitiesUrl(fixture.project().getId()) + "/{activityId}/read", selected.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result").doesNotExist());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(activitiesUrl(fixture.project().getId()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + selected.getId() + ")].isRead").value(true))
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + first.getId() + ")].isRead").value(false));

        assertThat(projectActivityReadRepository.findAll())
            .extracting(read -> read.getActivityLog().getId())
            .containsExactly(selected.getId());
    }

    @Test
    void 전체_확인은_현재_멤버의_모든_활동만_읽음으로_처리한다() throws Exception {
        // 전체 읽음은 본인의 모든 배지를 지우지만, 같은 프로젝트의 다른 멤버에게는 적용되면 안 된다.
        Fixture fixture = createFixture();
        Users green = saveUser("green@example.com", "그린", "google-green");
        projectMemberRepository.save(ProjectMember.createMember(fixture.project(), green));
        ActivityLog first = saveActivity(fixture.project(), fixture.chaTaehoon(), "차태훈님이 일정을 등록했어요.");
        ActivityLog second = saveActivity(fixture.project(), fixture.chaTaehoon(), "차태훈님이 피드백을 남겼어요.");
        entityManager.flush();

        mockMvc.perform(patch(activitiesUrl(fixture.project().getId()) + "/read-all")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(activitiesUrl(fixture.project().getId()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + first.getId() + ")].isRead").value(true))
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + second.getId() + ")].isRead").value(true));

        mockMvc.perform(get(activitiesUrl(fixture.project().getId()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(green)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + first.getId() + ")].isRead").value(false))
            .andExpect(jsonPath("$.result.items[?(@.activityId == " + second.getId() + ")].isRead").value(false));

        assertThat(projectActivityReadRepository.findAll())
            .hasSize(2)
            .extracting(ProjectActivityRead::getProjectMember)
            .allMatch(member -> member.getUser().getId().equals(fixture.chaTaehoon().getId()));
    }

    @Test
    void 다른_프로젝트의_활동을_읽음_처리하려_하면_찾을_수_없음으로_차단한다() throws Exception {
        // URL의 activityId만 바꿔 다른 프로젝트 활동을 확인 처리하는 수평 권한 우회를 막는다.
        Fixture fixture = createFixture();
        Project otherProject = saveProject(fixture.chaTaehoon(), "다른 프로젝트");
        projectMemberRepository.save(ProjectMember.createAdmin(otherProject, fixture.chaTaehoon()));
        ActivityLog otherProjectActivity = saveActivity(otherProject, fixture.chaTaehoon(), "다른 프로젝트의 활동");
        entityManager.flush();

        mockMvc.perform(patch(activitiesUrl(fixture.project().getId()) + "/{activityId}/read", otherProjectActivity.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.chaTaehoon())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("COMMON404"));

        assertThat(projectActivityReadRepository.findAll()).isEmpty();
    }

    private Fixture createFixture() {
        Users chaTaehoon = saveUser("chataehun@example.com", "차태훈", "google-chataehun");
        Project project = saveProject(chaTaehoon, "최근활동 HTTP 통합 테스트 프로젝트");
        projectMemberRepository.save(ProjectMember.createAdmin(project, chaTaehoon));

        return new Fixture(chaTaehoon, project);
    }

    private Users saveUser(String email, String nickname, String socialId) {
        return userRepository.save(Users.createSocialUser(email, nickname, null, SocialType.GOOGLE, socialId));
    }

    private Project saveProject(Users owner, String title) {
        return projectRepository.save(Project.create(
            owner,
            title,
            CategoryName.DOCUMENTARY,
            LengthType.SHORT_FORM,
            "최근활동 API HTTP 통합 검증용 프로젝트입니다.",
            LocalDate.now().plusDays(1),
            null,
            Kind.PERSONAL
        ));
    }

    private ActivityLog saveActivity(Project project, Users actor, String content) {
        return activityLogRepository.save(ActivityLog.create(
            project,
            ActivityActor.user(actor.getId(), actor.getNickname()),
            ActivityLogType.FILE_UPLOADED,
            content,
            ActivityLogTargetType.FILE,
            100L
        ));
    }

    private String activitiesUrl(Long projectId) {
        return "/api/v1/projects/" + projectId + "/activities";
    }

    private String bearerToken(Users user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId());
    }

    private record Fixture(Users chaTaehoon, Project project) {
    }
}
