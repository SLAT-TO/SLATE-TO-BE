package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "캘린더 일정 조회 응답")
public class ScheduleCalendarResponse {

    @Schema(description = "캘린더에 표시할 일정 목록")
    private List<CalendarSchedule> items;

    @Getter
    @Builder
    @Schema(description = "캘린더 일정 항목")
    public static class CalendarSchedule {

        @Schema(description = "일정 ID", example = "10")
        private Long scheduleId;

        @Schema(description = "일정 구분", example = "PROJECT")
        private ScheduleScope scheduleScope;

        @Schema(description = "프로젝트 ID. 개인 일정이면 null입니다.", example = "3", nullable = true)
        private Long projectId;

        @Schema(description = "프로젝트명. 개인 일정이면 null입니다.", example = "00 프로젝트", nullable = true)
        private String projectTitle;

        @Schema(description = "일정 제목", example = "레퍼런스 회의")
        private String title;

        @Schema(description = "시작 일시", example = "2026-07-05T14:00:00")
        private LocalDateTime startAt;

        @Schema(description = "종료 일시", example = "2026-07-05T15:00:00")
        private LocalDateTime endAt;

    }
}
