package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScheduleCalendarResponse {

    private List<CalendarSchedule> schedules;

    @Getter
    @Builder
    public static class CalendarSchedule {

        private Long scheduleId;

        private ScheduleScope scheduleScope;

        private Long projectId;

        private String projectTitle;

        private String title;

        private LocalDateTime startAt;

        private LocalDateTime endAt;

        private String color;
    }
}
