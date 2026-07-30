package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "일정 생성/수정 응답")
public class ScheduleResponse {

    @Schema(description = "일정 ID", example = "10")
    private Long scheduleId;

    @Schema(description = "일정 구분", example = "PROJECT")
    private ScheduleScope scheduleScope;

    @Schema(description = "프로젝트 ID. 개인 일정이면 null입니다.", example = "3", nullable = true)
    private Long projectId;

    @Schema(description = "일정 제목", example = "레퍼런스 회의")
    private String title;

    @Schema(description = "시작 일시", example = "2026-07-05T14:00:00")
    private LocalDateTime startAt;

    @Schema(description = "종료 일시", example = "2026-07-05T15:00:00")
    private LocalDateTime endAt;

    @Schema(description = "생성 일시", example = "2026-07-05T13:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2026-07-05T13:30:00")
    private LocalDateTime updatedAt;
}
