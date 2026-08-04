package com.slatto.domain.project.entity;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemberRecentActivityReadTest {

    @Test
    void 더_과거의_활동을_확인해도_마지막_확인_시각은_되돌아가지_않는다() {
        // 목록을 거슬러 읽더라도 이미 확인한 더 최신 활동을 다시 새 활동으로 만들면 안 된다.
        ProjectMember member = ProjectMember.createMember(null, createUser());
        LocalDateTime latestReadAt = LocalDateTime.of(2026, 8, 4, 12, 0);

        member.markActivitiesReadAt(latestReadAt);
        member.markActivitiesReadAt(latestReadAt.minusMinutes(10));

        assertThat(member.getLastActivityReadAt()).isEqualTo(latestReadAt);
    }

    @Test
    void 확인할_활동이_없으면_마지막_확인_시각을_변경하지_않는다() {
        // 빈 최근활동 목록에서 전체 읽음을 눌러도 임의의 현재 시각을 저장하지 않는다.
        ProjectMember member = ProjectMember.createMember(null, createUser());

        member.markActivitiesReadAt(null);

        assertThat(member.getLastActivityReadAt()).isNull();
    }

    private Users createUser() {
        return Users.createSocialUser(
            "green@example.com",
            "그린",
            null,
            SocialType.GOOGLE,
            "google-green"
        );
    }
}
