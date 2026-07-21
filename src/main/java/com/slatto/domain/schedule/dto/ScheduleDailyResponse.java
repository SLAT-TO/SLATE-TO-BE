package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScheduleDailyResponse {

    private LocalDate date;

    private List<DailySchedule> schedules;

    @Getter
    @Builder
    public static class DailySchedule {

        private Long scheduleId;

        private ScheduleScope scheduleScope;

        private Long projectId;

        private String projectTitle;

        private String title;

        private LocalDateTime startAt;

        private LocalDateTime endAt;

        private String location;

        private List<Participant> participants;

        private String participantSummary;

        private String publicMemo;

        private String privateMemo;

        private boolean canEdit;
    }

    @Getter
    @Builder
    public static class Participant {

        private Long userId;

        private String nickname;

        private String profileImageUrl;
    }
}
