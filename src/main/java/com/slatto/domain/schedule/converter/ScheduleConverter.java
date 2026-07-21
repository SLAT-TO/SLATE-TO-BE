package com.slatto.domain.schedule.converter;

import com.slatto.domain.schedule.dto.ScheduleResponse;
import com.slatto.domain.schedule.entity.Schedule;
import org.springframework.stereotype.Component;

@Component
public class ScheduleConverter {

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
}
