package com.slatto.domain.notification.service;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.enums.ActorType;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DataJpaTest
@Import({ActivityLogService.class, ActivityMessageFactory.class})
class ActivityLogServiceIntegrationTest {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void 회원_파일_등록_활동을_실제_최근활동_테이블에_저장한다() {
        // 서비스, 메시지 팩토리, JPA 엔티티를 함께 통과해 기존 회원 활동의 저장값 회귀를 확인한다.
        Users user = saveUser("member@example.com", "김수민");
        Project project = saveProject(user);

        activityLogService.createFileUploadedLog(project.getId(), user.getId(), 101L, "촬영계획서.pdf");

        ActivityLog activityLog = activityLogRepository.findAll().getFirst();
        assertThat(activityLog.getActorType()).isEqualTo(ActorType.USER);
        assertThat(activityLog.getActorUserId()).isEqualTo(user.getId());
        assertThat(activityLog.getActorGuestId()).isNull();
        assertThat(activityLog.getType()).isEqualTo(ActivityLogType.FILE_UPLOADED);
        assertThat(activityLog.getContent()).isEqualTo("김수민님이 [촬영계획서.pdf] 파일을 등록했어요");
        assertThat(activityLog.getTargetType()).isEqualTo(ActivityLogTargetType.FILE.name());
        assertThat(activityLog.getTargetId()).isEqualTo(101L);
    }

    @Test
    void 게스트_피드백_활동을_게스트_행위자로_저장한다() {
        // 공유 링크 게스트는 user ID 없이 guest ID와 CLIENT_REVIEWER 타입으로 저장돼야 한다.
        Users owner = saveUser("owner@example.com", "프로젝트 생성자");
        Project project = saveProject(owner);
        Guest guest = mock(Guest.class);
        given(guest.getId()).willReturn(202L);
        given(guest.getName()).willReturn("클라이언트");

        activityLogService.createGuestVideoFeedbackCommentedLog(project.getId(), guest, 303L, "버전 1");

        ActivityLog activityLog = activityLogRepository.findAll().getFirst();
        assertThat(activityLog.getActorType()).isEqualTo(ActorType.CLIENT_REVIEWER);
        assertThat(activityLog.getActorUserId()).isNull();
        assertThat(activityLog.getActorGuestId()).isEqualTo(202L);
        assertThat(activityLog.getType()).isEqualTo(ActivityLogType.VIDEO_FEEDBACK_COMMENTED);
        assertThat(activityLog.getContent()).isEqualTo("클라이언트님이 [버전 1]에 피드백을 남겼어요");
        assertThat(activityLog.getTargetType()).isEqualTo(ActivityLogTargetType.VIDEO.name());
        assertThat(activityLog.getTargetId()).isEqualTo(303L);
    }

    private Users saveUser(String email, String nickname) {
        return userRepository.save(Users.createSocialUser(
            email,
            nickname,
            null,
            SocialType.GOOGLE,
            "google-" + email
        ));
    }

    private Project saveProject(Users owner) {
        return projectRepository.save(Project.create(
            owner,
            "최근활동 테스트 프로젝트",
            CategoryName.DOCUMENTARY,
            LengthType.SHORT_FORM,
            "최근활동 저장 검증용 프로젝트입니다.",
            LocalDate.now().plusDays(1),
            null,
            Kind.PERSONAL
        ));
    }
}
