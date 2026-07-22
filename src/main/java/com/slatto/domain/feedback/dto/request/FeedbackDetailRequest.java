package com.slatto.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class FeedbackDetailRequest {

    @Schema(description = "답글 작성 요청")
    public record ReplyCreateReqDTO(
            @Schema(example = "1", nullable = true)
            Long userId,

            @Schema(example = "5", nullable = true)
            Long guestId,

            @Schema(example = "확인했습니다. 수정하겠습니다")
            @NotBlank(message = "내용은 필수입니다.")
            String content
    ) {
    }
}