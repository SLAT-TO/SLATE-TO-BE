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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int NOTIFICATION_RETENTION_HOURS = 24;
    private static final int GROUPING_LOCK_STRIPE_COUNT = 64;
    private static final Object[] GROUPING_LOCKS = createGroupingLocks();

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
        LocalDateTime updatedAfter = LocalDateTime.now().minusHours(NOTIFICATION_RETENTION_HOURS);
        Notification cursorNotification = resolveCursor(currentUserId, cursor, updatedAfter);

        List<Notification> notifications = notificationRepository.findRecentNotificationsByCursor(
            currentUserId,
            updatedAfter,
            cursor,
            getReadOrder(cursorNotification),
            getUpdatedAt(cursorNotification),
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

        int updatedCount = notificationRepository.markAsReadByIdAndUserId(
            notificationId,
            currentUserId,
            LocalDateTime.now()
        );
        if (updatedCount == 0) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
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

    /**
     * 전달받은 사용자 목록에 새 개인 알림을 생성한다.
     * 같은 대상의 기존 미읽음 알림이 있어도 새 알림으로 저장한다.
     */
    @Transactional
    public void createNotifications(NotificationCreateCommand command) {
        validateCreateCommand(command);

        Project project = getProjectOrNull(command.getProjectId());
        List<Long> recipientIds = getRecipientIds(command);
        if (recipientIds.isEmpty()) {
            return;
        }

        List<Notification> notifications = recipientIds.stream()
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

    /**
     * 동일 사용자, 알림 타입, targetType, targetId 기준으로 미읽음 알림을 그룹핑한다.
     * 기존 미읽음 알림이 있으면 content만 갱신하고, 없으면 새 알림을 생성한다.
     */
    @Transactional
    public void createOrUpdateGroupedNotifications(NotificationCreateCommand command) {
        validateCreateCommand(command);
        validateGroupingCommand(command);

        Project project = getProjectOrNull(command.getProjectId());
        List<Long> recipientIds = getRecipientIds(command);
        if (recipientIds.isEmpty()) {
            return;
        }

        recipientIds.stream()
            .map(this::getActiveUser)
            .forEach(recipient -> createOrUpdateGroupedNotification(recipient, project, command));
    }

    /**
     * 일정 담당자로 지정된 사용자에게 알림을 생성한다.
     * 일정 작성자는 excludeUserId로 제외한다.
     */
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

    /**
     * 프로젝트 초대 알림을 생성한다.
     * 클릭 대상은 프로젝트 상세 화면이다.
     */
    @Transactional
    public void createProjectInvitationNotification(
        Long recipientId,
        Long projectId,
        String content
    ) {
        validateRequiredId(recipientId);
        validateRequiredId(projectId);

        createNotification(NotificationCreateCommand.builder()
            .recipientIds(List.of(recipientId))
            .projectId(projectId)
            .type(NotificationType.PROJECT_INVITED)
            .content(content)
            .targetType(NotificationTargetType.PROJECT)
            .targetId(projectId)
            .build());
    }

    /**
     * 영상 피드백 댓글 알림을 생성하거나 기존 미읽음 알림을 갱신한다.
     * 동일 영상 기준으로 그룹핑하므로 targetId는 feedbackId가 아니라 videoId를 사용한다.
     */
    @Transactional
    public void createVideoFeedbackCommentedNotifications(
        Long projectId,
        Long videoId,
        String content,
        List<Long> recipientIds,
        Long actorUserId
    ) {
        validateRequiredId(projectId);
        validateRequiredId(videoId);

        createOrUpdateGroupedNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.VIDEO_FEEDBACK_COMMENTED)
            .content(content)
            .targetType(NotificationTargetType.VIDEO)
            .targetId(videoId)
            .excludeUserId(actorUserId)
            .build());
    }

    /**
     * 새로운 지원자 발생 알림을 생성하거나 기존 미읽음 알림을 갱신한다.
     * 동일 공고 기준으로 그룹핑하므로 targetId는 recruitmentId를 사용한다.
     */
    @Transactional
    public void createRecruitmentAppliedNotification(
        Long recipientId,
        Long recruitmentId,
        String content
    ) {
        validateRequiredId(recipientId);
        validateRequiredId(recruitmentId);

        createOrUpdateGroupedNotifications(NotificationCreateCommand.builder()
            .recipientIds(List.of(recipientId))
            .type(NotificationType.RECRUITMENT_APPLIED)
            .content(content)
            .targetType(NotificationTargetType.RECRUITMENT)
            .targetId(recruitmentId)
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
        LocalDateTime updatedAfter
    ) {
        if (cursor == null) {
            return null;
        }

        return notificationRepository.findByIdAndUserIdAndDeletedAtIsNullAndUpdatedAtGreaterThanEqual(
                cursor,
                currentUserId,
                updatedAfter
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
            || command.getRecipientIds() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void validateGroupingCommand(NotificationCreateCommand command) {
        if (command.getTargetType() == null || command.getTargetId() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void validateRequiredId(Long id) {
        if (id == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void createOrUpdateGroupedNotification(
        Users recipient,
        Project project,
        NotificationCreateCommand command
    ) {
        String targetType = getTargetTypeName(command.getTargetType());
        Object groupingLock = getGroupingLock(
            recipient.getId(),
            command.getType(),
            targetType,
            command.getTargetId()
        );

        synchronized (groupingLock) {
            // 먼저 기존 미읽음 그룹 알림 갱신을 시도하고, 없을 때만 새 알림을 생성한다.
            int updatedCount = notificationRepository.updateUnreadGroupedNotificationContent(
                recipient.getId(),
                command.getType(),
                targetType,
                command.getTargetId(),
                command.getContent(),
                LocalDateTime.now()
            );
            if (updatedCount > 0) {
                return;
            }

            notificationRepository.save(Notification.create(
                recipient,
                project,
                command.getType(),
                command.getContent(),
                targetType,
                command.getTargetId()
            ));
        }
    }

    private List<Long> getRecipientIds(NotificationCreateCommand command) {
        // 중복 수신자와 제외 대상 사용자를 정리해 실제 저장 대상만 남긴다.
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

    private LocalDateTime getUpdatedAt(Notification notification) {
        return notification != null ? notification.getUpdatedAt() : null;
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

    private Object getGroupingLock(
        Long recipientId,
        NotificationType type,
        String targetType,
        Long targetId
    ) {
        int lockIndex = Math.floorMod(
            Objects.hash(recipientId, type, targetType, targetId),
            GROUPING_LOCK_STRIPE_COUNT
        );

        return GROUPING_LOCKS[lockIndex];
    }

    private static Object[] createGroupingLocks() {
        Object[] locks = new Object[GROUPING_LOCK_STRIPE_COUNT];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }

        return locks;
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
