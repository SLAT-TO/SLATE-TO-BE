package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.NotificationCreateCommand;
import com.slatto.domain.notification.dto.NotificationListResponse;
import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.enums.NotificationTargetType;
import com.slatto.domain.notification.enums.NotificationType;
import com.slatto.domain.notification.repository.NotificationRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int NOTIFICATION_RETENTION_HOURS = 24;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

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
    public void createNotification(NotificationCreateCommand command) {
        createNotifications(command);
    }

    @Transactional
    public void createNotifications(NotificationCreateCommand command) {
        validateCreateCommand(command);

        Project project = getProjectOrNull(command.getProjectId());
        List<Notification> notifications = getRecipientIds(command).stream()
            .map(this::getActiveUser)
            .map(recipient -> Notification.create(
                recipient,
                project,
                command.getType(),
                command.getContent(),
                getTargetTypeName(command.getTargetType()),
                command.getTargetId()
            ))
            .toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional
    public void createOrUpdateGroupedNotifications(NotificationCreateCommand command) {
        validateCreateCommand(command);
        validateGroupingCommand(command);

        Project project = getProjectOrNull(command.getProjectId());
        List<Notification> newNotifications = getRecipientIds(command).stream()
            .map(this::getActiveUser)
            .map(recipient -> createOrUpdateGroupedNotification(recipient, project, command))
            .filter(Objects::nonNull)
            .toList();

        if (!newNotifications.isEmpty()) {
            notificationRepository.saveAll(newNotifications);
        }
    }

    @Transactional
    public void createScheduleAssignedNotifications(
        Project project,
        Long scheduleId,
        String scheduleTitle,
        List<Users> recipients,
        Long writerId
    ) {
        createNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipients.stream()
                .map(Users::getId)
                .toList())
            .projectId(project != null ? project.getId() : null)
            .type(NotificationType.SCHEDULE_ASSIGNED)
            .content(createScheduleAssignedContent(scheduleTitle))
            .targetType(NotificationTargetType.SCHEDULE)
            .targetId(scheduleId)
            .excludeUserId(writerId)
            .build());
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

    private Project getProjectOrNull(Long projectId) {
        if (projectId == null) {
            return null;
        }

        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private Users getActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private void validateCreateCommand(NotificationCreateCommand command) {
        if (command == null
            || command.getType() == null
            || command.getContent() == null
            || command.getContent().isBlank()
            || command.getRecipientIds() == null
            || command.getRecipientIds().isEmpty()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void validateGroupingCommand(NotificationCreateCommand command) {
        if (command.getTargetType() == null || command.getTargetId() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private Notification createOrUpdateGroupedNotification(
        Users recipient,
        Project project,
        NotificationCreateCommand command
    ) {
        String targetType = getTargetTypeName(command.getTargetType());
        Optional<Notification> existingNotification = notificationRepository
            .findTopByUserIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                recipient.getId(),
                command.getType(),
                targetType,
                command.getTargetId()
            );

        if (existingNotification.isPresent()) {
            existingNotification.get().updateContent(command.getContent());
            return null;
        }

        return Notification.create(
            recipient,
            project,
            command.getType(),
            command.getContent(),
            targetType,
            command.getTargetId()
        );
    }

    private List<Long> getRecipientIds(NotificationCreateCommand command) {
        Set<Long> recipientIds = new HashSet<>(command.getRecipientIds());
        recipientIds.remove(null);

        if (command.getExcludeUserId() != null) {
            recipientIds.removeIf(recipientId -> Objects.equals(recipientId, command.getExcludeUserId()));
        }

        return recipientIds.stream().toList();
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

    private String getTargetTypeName(NotificationTargetType targetType) {
        return targetType != null ? targetType.name() : null;
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
