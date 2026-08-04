package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.TodayBriefingResponse;
import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.enums.NotificationTargetType;
import com.slatto.domain.notification.enums.NotificationType;
import com.slatto.domain.notification.enums.TodayBriefingType;
import com.slatto.domain.notification.repository.NotificationRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.schedule.entity.Schedule;
import com.slatto.domain.schedule.repository.ScheduleRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayBriefingService {

    private static final int BRIEFING_LIMIT = 3;
    private static final int RECENT_NOTIFICATION_LOOKUP_SIZE = 20;
    private static final int BRIEFING_RETENTION_HOURS = 24;
    private static final List<Integer> START_REMINDER_DAY_OFFSETS = List.of(1, 3);
    private static final List<NotificationType> BRIEFING_NOTIFICATION_TYPES = List.of(
        NotificationType.RECRUITMENT_APPLIED,
        NotificationType.VIDEO_FEEDBACK_COMMENTED
    );

    private final ScheduleRepository scheduleRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public TodayBriefingResponse getTodayBriefings(Long currentUserId) {
        validateActiveUser(currentUserId);

        LocalDate today = LocalDate.now();
        LocalDateTime recentAfter = LocalDateTime.now().minusHours(BRIEFING_RETENTION_HOURS);
        List<BriefingCandidate> candidates = new ArrayList<>();

        addTodayScheduleBriefings(currentUserId, today, candidates);
        addStartReminderBriefings(currentUserId, today, candidates);
        addRecentNotificationBriefings(currentUserId, recentAfter, candidates);

        List<TodayBriefingResponse.BriefingItem> items = candidates.stream()
            .sorted(BRIEFING_ORDER)
            .limit(BRIEFING_LIMIT)
            .map(BriefingCandidate::toResponse)
            .toList();

        return TodayBriefingResponse.builder()
            .items(items)
            .build();
    }

    private void addTodayScheduleBriefings(
        Long currentUserId,
        LocalDate today,
        List<BriefingCandidate> candidates
    ) {
        List<Schedule> schedules = findAssignedSchedulesOnDate(currentUserId, today);

        schedules.forEach(schedule -> {
            candidates.add(BriefingCandidate.builder()
                .type(TodayBriefingType.TODAY_SCHEDULE)
                .content(buildTodayScheduleContent(schedule, today))
                .projectId(getProjectId(schedule.getProject()))
                .targetType(NotificationTargetType.SCHEDULE.name())
                .targetId(schedule.getId())
                .deadlineOrder(0)
                .occurredAt(schedule.getStartAt())
                .build());
        });
    }

    private void addStartReminderBriefings(
        Long currentUserId,
        LocalDate today,
        List<BriefingCandidate> candidates
    ) {
        for (Integer dayOffset : START_REMINDER_DAY_OFFSETS) {
            LocalDate startDate = today.plusDays(dayOffset);
            List<Schedule> schedules = findAssignedSchedulesStartingOnDate(currentUserId, startDate);

            schedules.forEach(schedule -> candidates.add(BriefingCandidate.builder()
                .type(TodayBriefingType.SCHEDULE_START_REMINDER)
                .content(buildStartReminderContent(schedule, dayOffset))
                .projectId(getProjectId(schedule.getProject()))
                .targetType(NotificationTargetType.SCHEDULE.name())
                .targetId(schedule.getId())
                .deadlineOrder(dayOffset)
                .occurredAt(schedule.getStartAt())
                .build()));
        }
    }

    private void addRecentNotificationBriefings(
        Long currentUserId,
        LocalDateTime recentAfter,
        List<BriefingCandidate> candidates
    ) {
        List<Notification> notifications = notificationRepository.findRecentBriefingCandidates(
            currentUserId,
            recentAfter,
            BRIEFING_NOTIFICATION_TYPES,
            PageRequest.of(0, RECENT_NOTIFICATION_LOOKUP_SIZE)
        );

        notifications.stream()
            .map(this::toNotificationCandidate)
            .forEach(candidates::add);
    }

    private List<Schedule> findAssignedSchedulesOnDate(Long currentUserId, LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        return scheduleRepository.findBriefingAssignedSchedulesBetween(
            currentUserId,
            startAt,
            endAt
        );
    }

    private List<Schedule> findAssignedSchedulesStartingOnDate(Long currentUserId, LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        return scheduleRepository.findBriefingAssignedSchedulesStartingBetween(
            currentUserId,
            startAt,
            endAt
        );
    }

    private BriefingCandidate toNotificationCandidate(Notification notification) {
        TodayBriefingType type = switch (notification.getType()) {
            case RECRUITMENT_APPLIED -> TodayBriefingType.RECRUITMENT_APPLIED;
            case VIDEO_FEEDBACK_COMMENTED -> TodayBriefingType.VIDEO_FEEDBACK_COMMENTED;
            default -> throw new BaseException(CommonErrorCode.BAD_REQUEST);
        };

        return BriefingCandidate.builder()
            .type(type)
            .content(notification.getContent())
            .projectId(getProjectId(notification.getProject()))
            .targetType(notification.getTargetType())
            .targetId(notification.getTargetId())
            .deadlineOrder(0)
            .occurredAt(notification.getUpdatedAt())
            .build();
    }

    private String buildTodayScheduleContent(Schedule schedule, LocalDate today) {
        String title = schedule.getTitle();
        String projectTitle = getProjectTitle(schedule.getProject());
        String actionText = isMultiDaySchedule(schedule) && isScheduleEndDate(schedule, today)
            ? "마감이에요"
            : "일정이 있어요";

        if (projectTitle == null) {
            return "오늘 [" + title + "] " + actionText;
        }
        return "[" + projectTitle + "] 오늘 [" + title + "] " + actionText;
    }

    private String buildStartReminderContent(Schedule schedule, int dayOffset) {
        String title = schedule.getTitle();
        String projectTitle = getProjectTitle(schedule.getProject());
        String reminderText = "D-" + dayOffset;

        if (projectTitle == null) {
            return "[" + title + "] 시작까지 " + reminderText;
        }
        return "[" + projectTitle + "] [" + title + "] 시작까지 " + reminderText;
    }

    private boolean isScheduleEndDate(Schedule schedule, LocalDate date) {
        return schedule.getEndAt().toLocalDate().isEqual(date);
    }

    private boolean isMultiDaySchedule(Schedule schedule) {
        return !schedule.getStartAt().toLocalDate().isEqual(schedule.getEndAt().toLocalDate());
    }

    private Long getProjectId(Project project) {
        return project == null ? null : project.getId();
    }

    private String getProjectTitle(Project project) {
        return project == null ? null : project.getTitle();
    }

    private void validateActiveUser(Long userId) {
        if (userId == null || !userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }

    private static final Comparator<BriefingCandidate> BRIEFING_ORDER = Comparator
        .comparingInt((BriefingCandidate candidate) -> candidate.getType().getPriority())
        .thenComparingInt(BriefingCandidate::getDeadlineOrder)
        .thenComparing(BriefingCandidate::getOccurredAt, Comparator.reverseOrder());

    @Getter
    @Builder
    private static class BriefingCandidate {

        private TodayBriefingType type;
        private String content;
        private Long projectId;
        private String targetType;
        private Long targetId;
        private int deadlineOrder;
        private LocalDateTime occurredAt;

        private TodayBriefingResponse.BriefingItem toResponse() {
            return TodayBriefingResponse.BriefingItem.builder()
                .type(type)
                .content(content)
                .priority(type.getPriority())
                .projectId(projectId)
                .targetType(targetType)
                .targetId(targetId)
                .occurredAt(occurredAt)
                .build();
        }
    }
}
