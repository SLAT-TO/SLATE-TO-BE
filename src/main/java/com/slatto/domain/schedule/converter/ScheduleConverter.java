package com.slatto.domain.schedule.converter;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.schedule.dto.ScheduleCalendarResponse;
import com.slatto.domain.schedule.dto.ScheduleResponse;
import com.slatto.domain.schedule.entity.Schedule;
import com.slatto.domain.schedule.enums.ScheduleScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleConverter {

    private static final String PERSONAL_SCHEDULE_COLOR = "#4F8BFF";
    private static final String PROJECT_SCHEDULE_COLOR = "#2BB673";

    public ScheduleResponse toResponse(Schedule schedule) {
        Long projectId = schedule.getProject() != null
            ? schedule.getProject().getId()
            : null;

        return ScheduleResponse.builder()
            .scheduleId(schedule.getId())
            .scheduleScope(schedule.getScheduleScope())
            .projectId(projectId)
            .title(schedule.getTitle())
            .startAt(schedule.getStartAt())
            .endAt(schedule.getEndAt())
            .createdAt(schedule.getCreatedAt())
            .updatedAt(schedule.getUpdatedAt())
            .build();
    }

    public ScheduleCalendarResponse toCalendarResponse(List<Schedule> schedules) {
        List<ScheduleCalendarResponse.CalendarSchedule> calendarSchedules = schedules.stream()
            .map(this::toCalendarSchedule)
            .toList();

        return ScheduleCalendarResponse.builder()
            .schedules(calendarSchedules)
            .build();
    }

    private ScheduleCalendarResponse.CalendarSchedule toCalendarSchedule(Schedule schedule) {
        Project project = schedule.getProject();

        return ScheduleCalendarResponse.CalendarSchedule.builder()
            .scheduleId(schedule.getId())
            .scheduleScope(schedule.getScheduleScope())
            .projectId(project != null ? project.getId() : null)
            .projectTitle(project != null ? project.getTitle() : null)
            .title(schedule.getTitle())
            .startAt(schedule.getStartAt())
            .endAt(schedule.getEndAt())
            .color(resolveCalendarColor(schedule.getScheduleScope()))
            .build();
    }

    private String resolveCalendarColor(ScheduleScope scheduleScope) {
        return scheduleScope == ScheduleScope.PERSONAL
            ? PERSONAL_SCHEDULE_COLOR
            : PROJECT_SCHEDULE_COLOR;
    }
}
