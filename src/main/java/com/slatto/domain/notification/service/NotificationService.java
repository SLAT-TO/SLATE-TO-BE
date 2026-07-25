package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.NotificationListResponse;
import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.enums.NotificationType;
import com.slatto.domain.notification.repository.NotificationRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final String TARGET_TYPE_SCHEDULE = "SCHEDULE";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int NOTIFICATION_RETENTION_HOURS = 24;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationListResponse getNotifications(
        Long currentUserId,
        Long cursor,
        int size
    ) {
        validateActiveUser(currentUserId);

        int pageSize = normalizePageSize(size);
        LocalDateTime createdAfter = LocalDateTime.now().minusHours(NOTIFICATION_RETENTION_HOURS);
        Notification cursorNotification = resolveCursor(currentUserId, cursor, createdAfter);

        List<Notification> notifications = notificationRepository.findRecentNotificationsByCursor(
            currentUserId,
            createdAfter,
            cursor,
            getReadOrder(cursorNotification),
            getCreatedAt(cursorNotification),
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = notifications.size() > pageSize;
        List<Notification> currentPageNotifications = notifications.stream()
            .limit(pageSize)
            .toList();

        List<NotificationListResponse.NotificationSummary> items = currentPageNotifications.stream()
            .map(this::toSummary)
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).getNotificationId()
            : null;

        return NotificationListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    @Transactional
    public void markNotificationAsRead(Long currentUserId, Long notificationId) {
        validateActiveUser(currentUserId);

        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(
                notificationId,
                currentUserId
            )
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        notification.markAsRead();
    }

    @Transactional
    public void markAllNotificationsAsRead(Long currentUserId) {
        validateActiveUser(currentUserId);

        notificationRepository.markAllAsReadByUserId(currentUserId, LocalDateTime.now());
    }

    @Transactional
    public void createScheduleAssignedNotifications(
        Project project,
        Long scheduleId,
        String scheduleTitle,
        List<Users> recipients,
        Long writerId
    ) {
        List<Notification> notifications = recipients.stream()
            .filter(recipient -> !Objects.equals(recipient.getId(), writerId))
            .map(recipient -> Notification.create(
                recipient,
                project,
                NotificationType.SCHEDULE_ASSIGNED,
                createScheduleAssignedContent(scheduleTitle),
                TARGET_TYPE_SCHEDULE,
                scheduleId
            ))
            .toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private void validateActiveUser(Long currentUserId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(currentUserId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }

    private Notification resolveCursor(
        Long currentUserId,
        Long cursor,
        LocalDateTime createdAfter
    ) {
        if (cursor == null) {
            return null;
        }

        return notificationRepository.findByIdAndUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                cursor,
                currentUserId,
                createdAfter
            )
            .orElseThrow(() -> new BaseException(CommonErrorCode.BAD_REQUEST));
    }

    private Integer getReadOrder(Notification notification) {
        if (notification == null) {
            return null;
        }

        return Boolean.FALSE.equals(notification.getIsRead()) ? 0 : 1;
    }

    private LocalDateTime getCreatedAt(Notification notification) {
        return notification != null ? notification.getCreatedAt() : null;
    }

    private NotificationListResponse.NotificationSummary toSummary(Notification notification) {
        Long projectId = notification.getProject() != null
            ? notification.getProject().getId()
            : null;

        return NotificationListResponse.NotificationSummary.builder()
            .notificationId(notification.getId())
            .projectId(projectId)
            .type(notification.getType())
            .content(notification.getContent())
            .targetType(notification.getTargetType())
            .targetId(notification.getTargetId())
            .isRead(notification.getIsRead())
            .readAt(notification.getReadAt())
            .createdAt(notification.getCreatedAt())
            .build();
    }

    private String createScheduleAssignedContent(String scheduleTitle) {
        return "'" + scheduleTitle + "' 일정 담당자로 지정되었습니다.";
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
