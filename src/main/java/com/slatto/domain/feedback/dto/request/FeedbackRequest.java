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

            @Schema(example = "00:15 자막 오타 있어요")
            @NotBlank(message = "내용은 필수입니다.")
            String content
    ) {
    }
}