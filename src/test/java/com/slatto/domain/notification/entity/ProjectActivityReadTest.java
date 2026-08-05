package com.slatto.domain.notification.entity;

import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.model.ActivityActor;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectActivityReadTest {

    @Test
    void 읽음_상태는_프로젝트_멤버와_활동_로그_한_건을_연결한다() {
        // 개별 읽음은 멤버별·활동별 행으로 저장되어 다른 활동의 읽음 상태를 바꾸지 않는다.
        Users green = Users.createSocialUser(
            "green@example.com",
            "그린",
            null,
            SocialType.GOOGLE,
            "google-green"
        );
        ProjectMember member = ProjectMember.createMember(null, green);
        ActivityLog activityLog = ActivityLog.create(
            null,
            ActivityActor.user(1L, "차태훈"),
            ActivityLogType.NOTICE_CREATED,
            "차태훈님이 새 공지를 등록했어요.",
            ActivityLogTargetType.NOTICE,
            1L
        );

        ProjectActivityRead activityRead = ProjectActivityRead.create(member, activityLog);

        assertThat(activityRead.getProjectMember()).isSameAs(member);
        assertThat(activityRead.getActivityLog()).isSameAs(activityLog);
        assertThat(activityRead.getReadAt()).isNotNull();
    }
}
