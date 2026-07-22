package com.slatto.domain.feedback.dto.response;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.sharelink.entity.Guest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

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
    ) {  }

    @Schema(description = "피드백 수정 응답")
    public record FeedbackUpdateResDTO(
            @Schema(example = "1") Long feedbackId,
            @Schema(example = "10") Long videoId,
            ActorDTO actor,
            @Schema(example = "수정된 내용입니다") String content,
            @Schema(example = "30", nullable = true) Long startTime,
            @Schema(example = "35", nullable = true) Long endTime,
            @Schema(example = "true") Boolean status,
            LocalDateTime updatedAt
    ) {   }

    @Schema(description = "피드백 목록 항목")
    public record FeedbackListItemDTO(
            @Schema(example = "1") Long feedbackId,
            @Schema(example = "10") Long videoId,
            ActorDTO actor,
            @Schema(example = "이 부분 색감 보정 부탁드려요") String content,
            @Schema(example = "24", nullable = true) Long startTime,
            @Schema(example = "27", nullable = true) Long endTime,
            @Schema(example = "false") Boolean status,
            LocalDateTime createdAt
    ) { }

    @Schema(description = "피드백 목록 응답")
    public record FeedbackListResDTO(
            List<FeedbackListItemDTO> items,
            @Schema(example = "24_5", nullable = true) String nextCursor,
            @Schema(example = "true") Boolean hasNext
    ) { }

    @Schema(description = "피드백 해결 상태 변경 응답")
    public record FeedbackStatusResDTO(
            @Schema(example = "1") Long feedbackId,
            @Schema(example = "true") Boolean status,
            LocalDateTime updatedAt
    ) { }
}