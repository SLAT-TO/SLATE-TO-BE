package com.slatto.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    ) { }

    @Schema(description = "피드백 수정 요청")
    public record FeedbackUpdateReqDTO(
            @Schema(example = "1", nullable = true)
            Long userId,

            @Schema(example = "5", nullable = true)
            Long guestId,

            @Schema(example = "수정된 내용입니다", nullable = true)
            String content,

            @Schema(example = "30", nullable = true)
            Long startTime,

            @Schema(example = "35", nullable = true)
            Long endTime,

            @Schema(example = "true", nullable = true)
            Boolean status
    ) { }

    @Schema(description = "피드백 해결 상태 변경 요청")
    public record FeedbackStatusReqDTO(
            @Schema(example = "1")
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @Schema(example = "true")
            @NotNull(message = "상태는 필수입니다.")
            Boolean status
    ) {
    }
}