package com.slatto.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "일정 개인 메모 저장/수정 응답")
public class SchedulePrivateMemoResponse {

    @Schema(description = "개인 메모 ID", example = "5")
    private Long privateMemoId;

    @Schema(description = "일정 ID", example = "10")
    private Long scheduleId;

    @Schema(description = "개인 메모 내용", example = "회의 전에 레퍼런스 링크 확인")
    private String content;

    @Schema(description = "수정 일시", example = "2026-07-05T15:30:00")
    private LocalDateTime updatedAt;
}
