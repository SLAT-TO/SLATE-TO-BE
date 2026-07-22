package com.slatto.domain.feedback.dto.response;

import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.sharelink.entity.Guest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FeedbackResponse {

    // actor(작성자) 담는 그릇
    @Schema(description = "작성자 정보")
    public record ActorDTO(
            @Schema(example = "USER") String type,   // "USER" 또는 "GUEST"
            @Schema(example = "20") Long id,
            @Schema(example = "김수민") String name
    ) {
        // 회원이면 USER actor 만들기
        public static ActorDTO fromUser(Users user) {
            return new ActorDTO("USER", user.getId(), user.getNickname());
        }

        // 게스트면 GUEST actor 만들기
        public static ActorDTO fromGuest(Guest guest) {
            return new ActorDTO("GUEST", guest.getId(), guest.getName());
        }
    }

    @Schema(description = "피드백 작성 응답")
    public record FeedbackCreateResDTO(
            @Schema(example = "1") Long feedbackId,
            @Schema(example = "10") Long videoId,
            ActorDTO actor,                                  // userId/guestId → actor
            @Schema(example = "이 부분 색감 보정 부탁드려요") String content,
            @Schema(example = "24", nullable = true) Long startTime,
            @Schema(example = "27", nullable = true) Long endTime,
            @Schema(example = "false") Boolean status,
            LocalDateTime createdAt
    ) {


    }
}