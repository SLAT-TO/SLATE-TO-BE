package com.slatto.domain.schedule.converter;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.schedule.dto.ScheduleCalendarResponse;
import com.slatto.domain.schedule.dto.ScheduleDailyResponse;
import com.slatto.domain.schedule.dto.ScheduleResponse;
import com.slatto.domain.schedule.entity.Schedule;
import com.slatto.domain.schedule.entity.ScheduleParticipant;
import com.slatto.domain.schedule.enums.ScheduleScope;
import com.slatto.domain.user.entity.Users;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    public ScheduleDailyResponse toDailyResponse(
        LocalDate date,
        Long currentUserId,
        List<Schedule> schedules,
        Map<Long, List<ScheduleParticipant>> participantsByScheduleId,
        Map<Long, String> privateMemoByScheduleId
    ) {
        List<ScheduleDailyResponse.DailySchedule> dailySchedules = schedules.stream()
            .map(schedule -> toDailySchedule(
                schedule,
                currentUserId,
                participantsByScheduleId.getOrDefault(schedule.getId(), List.of()),
                privateMemoByScheduleId.get(schedule.getId())
            ))
            .toList();

        return ScheduleDailyResponse.builder()
            .date(date)
            .schedules(dailySchedules)
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

    private ScheduleDailyResponse.DailySchedule toDailySchedule(
        Schedule schedule,
        Long currentUserId,
        List<ScheduleParticipant> participants,
        String privateMemo
    ) {
        Project project = schedule.getProject();
        List<ScheduleDailyResponse.Participant> responseParticipants = participants.stream()
            .map(ScheduleParticipant::getUser)
            .map(this::toDailyParticipant)
            .toList();

        return ScheduleDailyResponse.DailySchedule.builder()
            .scheduleId(schedule.getId())
            .scheduleScope(schedule.getScheduleScope())
            .projectId(project != null ? project.getId() : null)
            .projectTitle(project != null ? project.getTitle() : null)
            .title(schedule.getTitle())
            .startAt(schedule.getStartAt())
            .endAt(schedule.getEndAt())
            .location(schedule.getLocation())
            .participants(responseParticipants)
            .participantSummary(resolveParticipantSummary(responseParticipants))
            .publicMemo(schedule.getPublicMemo())
            .privateMemo(privateMemo)
            .canEdit(canEditSchedule(schedule, currentUserId, participants))
            .build();
    }

    private ScheduleDailyResponse.Participant toDailyParticipant(Users user) {
        return ScheduleDailyResponse.Participant.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }

    private String resolveParticipantSummary(List<ScheduleDailyResponse.Participant> participants) {
        if (participants.isEmpty()) {
            return "";
        }

        if (participants.size() == 1) {
            return participants.get(0).getNickname();
        }

        return participants.get(0).getNickname() + " 외 " + (participants.size() - 1) + "명";
    }

    private boolean canEditSchedule(
        Schedule schedule,
        Long currentUserId,
        List<ScheduleParticipant> participants
    ) {
        if (schedule.getScheduleScope() == ScheduleScope.PERSONAL) {
            return schedule.isWriter(currentUserId);
        }

        return participants.stream()
            .map(ScheduleParticipant::getUser)
            .anyMatch(user -> user.getId().equals(currentUserId));
    }
}
