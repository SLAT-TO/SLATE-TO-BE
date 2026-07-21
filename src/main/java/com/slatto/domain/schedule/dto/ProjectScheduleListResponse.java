package com.slatto.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProjectScheduleListResponse {

    private List<ProjectSchedule> schedules;

    @Getter
    @Builder
    public static class ProjectSchedule {

        private Long scheduleId;

        private String title;

        private LocalDateTime startAt;

        private LocalDateTime endAt;

        private String location;

        private String participantSummary;

        private String publicMemo;

        private boolean canEdit;
    }
}
