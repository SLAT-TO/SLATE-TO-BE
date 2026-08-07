package com.slatto.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.notification.dto.ActivityLogListResponse;
import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadCommandRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadRepository;
import com.slatto.domain.project.dto.ProjectNoticeCreateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectNoticeReadRepository;
import com.slatto.domain.project.repository.ProjectNoticeRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.project.service.ProjectNoticeService;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
    ProjectNoticeService.class,
    ProjectAccessValidator.class,
    ActivityLogService.class,
    ActivityMessageFactory.class,
    RecentActivityService.class,
    ProjectActivityReadCommandRepository.class
})
class ProjectNoticeActivityFlowIntegrationTest {

    @Autowired
    private ProjectNoticeService projectNoticeService;

    @Autowired
    private RecentActivityService recentActivityService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ProjectActivityReadRepository projectActivityReadRepository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 공지_등록은_최근활동으로_저장되고_다른_프로젝트_멤버에게_새_활동으로_조회된다() {
        // 실제 공지 서비스 호출부터 activity_log 저장, 다른 멤버의 목록 노출까지 하나의 트랜잭션 흐름을 검증한다.
        Users chaTaehoon = saveUser("chataehun@example.com", "차태훈");
        Users green = saveUser("green@example.com", "그린");
        Project project = saveProject(chaTaehoon);
        projectMemberRepository.save(ProjectMember.createAdmin(project, chaTaehoon));
        projectMemberRepository.save(ProjectMember.createMember(project, green));

        projectNoticeService.createProjectNotice(
            project.getId(),
            chaTaehoon.getId(),
            createNoticeRequest("촬영 일정 안내", "촬영은 금요일 오전 10시에 시작합니다.")
        );

        ActivityLog activityLog = activityLogRepository.findAll().getFirst();
        ActivityLogListResponse response = recentActivityService.getRecentActivities(
            project.getId(), green.getId(), null, 20
        );

        assertThat(activityLog.getType()).isEqualTo(ActivityLogType.NOTICE_CREATED);
        assertThat(activityLog.getTargetType()).isEqualTo(ActivityLogTargetType.NOTICE.name());
        assertThat(activityLog.getActorUserId()).isEqualTo(chaTaehoon.getId());
        assertThat(activityLog.getContent()).isEqualTo("차태훈님이 새 공지를 등록했어요");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.activityId()).isEqualTo(activityLog.getId());
            assertThat(item.isRead()).isFalse();
            assertThat(item.targetType()).isEqualTo(ActivityLogTargetType.NOTICE.name());
            assertThat(item.targetId()).isEqualTo(activityLog.getTargetId());
        });
        assertThat(projectActivityReadRepository.findAll()).isEmpty();
    }

    private ProjectNoticeCreateRequest createNoticeRequest(String title, String content) {
        try {
            // 실제 Controller의 JSON 역직렬화와 같은 방식으로 protected DTO 생성 경로를 통과한다.
            return objectMapper.readValue(
                objectMapper.writeValueAsString(Map.of("title", title, "content", content)),
                ProjectNoticeCreateRequest.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Users saveUser(String email, String nickname) {
        return userRepository.save(Users.createSocialUser(
            email,
            nickname,
            null,
            SocialType.GOOGLE,
            "google-" + nickname
        ));
    }

    private Project saveProject(Users owner) {
        return projectRepository.save(Project.create(
            owner,
            "최근활동 도메인 통합 테스트 프로젝트",
            CategoryName.DOCUMENTARY,
            LengthType.SHORT_FORM,
            "도메인 이벤트와 최근활동 연결을 검증합니다.",
            LocalDate.now().plusDays(1),
            null,
            Kind.PERSONAL
        ));
    }
}
