package com.slatto.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class FeedbackRequest {

    @Schema(description = "피드백 작성 요청")
    public record FeedbackCreateReqDTO(
            @Schema(example = "5", nullable = true, description = "게스트 작성 시에만 사용. 회원은 JWT로 식별")
            Long guestId,

            @Schema(example = "이 부분 색감 보정 부탁드려요")
            @NotBlank(message = "내용은 필수입니다.")
            String content,

            @Schema(example = "24", nullable = true)
            @PositiveOrZero(message = "시작 시간은 0 이상이어야 합니다.")
            Long startTime,

            @Schema(example = "27", nullable = true)
            @PositiveOrZero(message = "종료 시간은 0 이상이어야 합니다.")
            Long endTime
    ) { }

    @Schema(description = "피드백 수정 요청")
    public record FeedbackUpdateReqDTO(
            @Schema(example = "5", nullable = true, description = "게스트 작성 시에만 사용. 회원은 JWT로 식별")
            Long guestId,

            @Schema(example = "수정된 내용입니다", nullable = true)
            String content,

            @Schema(example = "30", nullable = true)
            @PositiveOrZero(message = "시작 시간은 0 이상이어야 합니다.")
            Long startTime,

            @Schema(example = "35", nullable = true)
            @PositiveOrZero(message = "종료 시간은 0 이상이어야 합니다.")
            Long endTime
    ) { }

    @Schema(description = "피드백 해결 상태 변경 요청")
    public record FeedbackStatusReqDTO(
            @Schema(example = "true")
            @NotNull(message = "상태는 필수입니다.")
            Boolean status
    ) { }
}