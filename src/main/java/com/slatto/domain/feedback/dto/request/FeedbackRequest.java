package com.slatto.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class FeedbackRequest {

    @Schema(description = "피드백 작성 요청")
    public record FeedbackCreateReqDTO(
            @Schema(example = "1", nullable = true)
            Long userId,

            @Schema(example = "5", nullable = true)
            Long guestId,

            @Schema(example = "이 부분 색감 보정 부탁드려요")
            @NotBlank(message = "내용은 필수입니다.")
            String content,
            
            @Schema(example = "24", nullable = true)
            Long startTime,

            @Schema(example = "27", nullable = true)
            Long endTime

    ) {
    }
}