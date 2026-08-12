package com.slatto.domain.notification.service;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.enums.ActorType;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        Users user = saveUser("green@example.com", "그린");
        Project project = saveProject(user);

        activityLogService.createFileUploadedLog(project.getId(), user.getId(), 101L, "촬영계획서.pdf");

        ActivityLog activityLog = activityLogRepository.findAll().getFirst();
        assertThat(activityLog.getActorType()).isEqualTo(ActorType.USER);
        assertThat(activityLog.getActorUserId()).isEqualTo(user.getId());
        assertThat(activityLog.getActorGuestId()).isNull();
        assertThat(activityLog.getType()).isEqualTo(ActivityLogType.FILE_UPLOADED);
        assertThat(activityLog.getContent()).isEqualTo("그린님이 [촬영계획서.pdf] 파일을 등록했어요");
        assertThat(activityLog.getTargetType()).isEqualTo(ActivityLogTargetType.FILE.name());
        assertThat(activityLog.getTargetId()).isEqualTo(101L);
    }

    @Test
    void 게스트_피드백_활동을_게스트_행위자로_저장한다() {
        // 공유 링크 게스트는 user ID 없이 guest ID와 CLIENT_REVIEWER 타입으로 저장돼야 한다.
        Users owner = saveUser("chataehun@example.com", "차태훈");
        Project project = saveProject(owner);
        Guest guest = mock(Guest.class);
        given(guest.getId()).willReturn(202L);
        given(guest.getName()).willReturn("차태훈");

        activityLogService.createGuestVideoFeedbackCommentedLog(project.getId(), guest, 303L, "버전 1");

        ActivityLog activityLog = activityLogRepository.findAll().getFirst();
        assertThat(activityLog.getActorType()).isEqualTo(ActorType.CLIENT_REVIEWER);
        assertThat(activityLog.getActorUserId()).isNull();
        assertThat(activityLog.getActorGuestId()).isEqualTo(202L);
        assertThat(activityLog.getType()).isEqualTo(ActivityLogType.VIDEO_FEEDBACK_COMMENTED);
        assertThat(activityLog.getContent()).isEqualTo("차태훈님이 [버전 1]에 피드백을 남겼어요");
        assertThat(activityLog.getTargetType()).isEqualTo(ActivityLogTargetType.VIDEO.name());
        assertThat(activityLog.getTargetId()).isEqualTo(303L);
    }

    @Test
    void 프로젝트_수정과_상태_변경_활동을_각각_저장한다() {
        // 프로젝트 서비스가 두 이벤트를 연속 호출했을 때, 최근활동 저장소에는 유형과 대상이 보존돼야 한다.
        Users user = saveUser("green@example.com", "그린");
        Project project = saveProject(user);

        activityLogService.createProjectUpdatedLog(project.getId(), user.getId());
        // 화면 표기와 어긋나면 최근활동에만 다른 단계 이름이 찍히므로, 문구는 enum 라벨에서 가져온다.
        activityLogService.createProjectStatusChangedLog(
            project.getId(), user.getId(),
            ProjectStatus.PREPARING.getLabel(), ProjectStatus.EDITING.getLabel()
        );

        List<ActivityLog> activityLogs = activityLogRepository.findAll();
        assertThat(activityLogs)
            .extracting(ActivityLog::getType)
            .containsExactlyInAnyOrder(
                ActivityLogType.PROJECT_UPDATED,
                ActivityLogType.PROJECT_STATUS_CHANGED
            );
        assertThat(activityLogs)
            .extracting(ActivityLog::getContent)
            .contains("그린님이 프로젝트 정보를 수정했어요", "그린님이 프로젝트 단계를 '기획 중'에서 '편집 중'으로 변경했어요");
    }

    @Test
    void 필수_파일명이_비어있으면_활동을_저장하지_않는다() {
        // 업로드 처리 중 파일명이 비정상이라면 BAD_REQUEST로 중단하고, 화면에 보일 잘못된 활동 로그도 남기면 안 된다.
        Users chaTaehoon = saveUser("chataehun@example.com", "차태훈");
        Project project = saveProject(chaTaehoon);

        assertThatThrownBy(() -> activityLogService.createFileUploadedLog(
            project.getId(), chaTaehoon.getId(), 101L, "  "
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(CommonErrorCode.BAD_REQUEST);

        assertThat(activityLogRepository.findAll()).isEmpty();
    }

    @Test
    void 존재하지_않는_프로젝트에는_활동을_저장하지_않는다() {
        // 이미 삭제됐거나 잘못된 프로젝트 ID에 대한 이벤트는 NOT_FOUND로 차단하고 다른 프로젝트 로그를 만들면 안 된다.
        Users green = saveUser("green@example.com", "그린");

        assertThatThrownBy(() -> activityLogService.createProjectUpdatedLog(999_999L, green.getId()))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(CommonErrorCode.NOT_FOUND);

        assertThat(activityLogRepository.findAll()).isEmpty();
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
