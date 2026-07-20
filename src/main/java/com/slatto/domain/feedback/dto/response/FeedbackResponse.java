package com.slatto.domain.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FeedbackResponse {

    @Schema(description = "피드백 작성 응답")
    public record FeedbackCreateResDTO(
            @Schema(example = "1") Long feedbackId,
            @Schema(example = "10") Long videoId,
            @Schema(example = "1", nullable = true) Long userId,
            @Schema(example = "5", nullable = true) Long guestId,
            @Schema(example = "00:15 자막 오타 있어요") String content,
            @Schema(example = "false") Boolean status,
            LocalDateTime createdAt
    ) {
    }
}