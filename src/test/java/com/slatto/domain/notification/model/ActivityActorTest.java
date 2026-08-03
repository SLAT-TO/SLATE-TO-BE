package com.slatto.domain.notification.model;

import com.slatto.domain.notification.enums.ActorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityActorTest {

    @Test
    void 회원_행위자는_사용자_ID만_저장한다() {
        // 회원 활동은 actor_user_id만 채워져야 게스트 활동과 구분할 수 있다.
        ActivityActor actor = ActivityActor.user(1L, "김수민");

        assertThat(actor.actorType()).isEqualTo(ActorType.USER);
        assertThat(actor.actorUserId()).isEqualTo(1L);
        assertThat(actor.actorGuestId()).isNull();
        assertThat(actor.displayName()).isEqualTo("김수민");
    }

    @Test
    void 게스트_행위자는_게스트_ID만_저장한다() {
        // 공유 링크 게스트의 활동은 actor_guest_id만 채워져야 한다.
        ActivityActor actor = ActivityActor.clientReviewer(2L, "클라이언트");

        assertThat(actor.actorType()).isEqualTo(ActorType.CLIENT_REVIEWER);
        assertThat(actor.actorUserId()).isNull();
        assertThat(actor.actorGuestId()).isEqualTo(2L);
        assertThat(actor.displayName()).isEqualTo("클라이언트");
    }

    @Test
    void 행위자_타입과_ID_조합이_맞지_않으면_생성을_막는다() {
        // 회원 활동에 guest ID를 함께 넣는 식의 잘못된 저장을 모델 경계에서 차단한다.
        assertThatThrownBy(() -> new ActivityActor(ActorType.USER, 1L, 2L, "김수민"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
