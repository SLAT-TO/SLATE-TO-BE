package com.slatto.domain.feedback.dto.response;

import com.slatto.domain.feedback.dto.response.FeedbackResponse.ActorDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class FeedbackDetailResponse {

    @Schema(description = "답글 작성 응답")
    public record ReplyCreateResDTO(
            @Schema(example = "1") Long replyId,
            @Schema(example = "10") Long feedbackId,
            ActorDTO actor,
            @Schema(example = "확인했습니다. 수정하겠습니다") String content,
            @Schema(example = "false") Boolean status,
            LocalDateTime createdAt
    ) {
    }
}