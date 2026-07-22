package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.NotificationListResponse;
import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.repository.NotificationRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

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
        Notification cursorNotification = resolveCursor(currentUserId, cursor);

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

    private void validateActiveUser(Long currentUserId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(currentUserId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }

    private Notification resolveCursor(Long currentUserId, Long cursor) {
        if (cursor == null) {
            return null;
        }

        return notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(cursor, currentUserId)
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

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
