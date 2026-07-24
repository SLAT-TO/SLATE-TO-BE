package com.slatto.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    @Schema(description = "답글 수정 요청")
    public record ReplyUpdateReqDTO(
            @Schema(example = "1", nullable = true)
            Long userId,

            @Schema(example = "5", nullable = true)
            Long guestId,

            @Schema(example = "수정된 답글입니다")
            @NotBlank(message = "내용은 필수입니다.")
            String content
    ) {
    }

    @Schema(description = "답글 해결 상태 변경 요청")
    public record ReplyStatusReqDTO(
            @Schema(example = "1")
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @Schema(example = "true")
            @NotNull(message = "상태는 필수입니다.")
            Boolean status
    ) {
    }
}