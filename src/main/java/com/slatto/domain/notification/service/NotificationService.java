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
import java.util.function.IntFunction;

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

    /**
     * 영상 카드의 미읽은 피드백 개수에 사용할 그룹 알림 누적 개수를 조회한다.
     */
    public int getUnreadVideoFeedbackCount(Long currentUserId, Long videoId) {
        validateActiveUser(currentUserId);
        validateRequiredId(videoId);

        return notificationRepository.findUnreadGroupedNotificationCount(
            currentUserId,
            NotificationType.VIDEO_FEEDBACK_COMMENTED,
            NotificationTargetType.VIDEO.name(),
            videoId
        );
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
                command.getTitle(),
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
     * 기존 미읽음 알림이 있으면 제목, 문구, 누적 개수를 갱신하고, 없으면 새 알림을 생성한다.
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
        validateRequiredId(scheduleId);
        validateRequiredText(scheduleTitle);
        if (recipients == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        Project notificationProject = getProjectOrNull(project != null ? project.getId() : null);
        String title = createScheduleAssignedTitle(project != null ? project.getTitle() : null);
        String targetType = getTargetTypeName(NotificationTargetType.SCHEDULE);

        List<Notification> notifications = recipients.stream()
            .filter(Objects::nonNull)
            .filter(recipient -> !Objects.equals(recipient.getId(), writerId))
            .map(recipient -> Notification.create(
                recipient,
                notificationProject,
                NotificationType.SCHEDULE_ASSIGNED,
                title,
                createScheduleAssignedContent(scheduleTitle, recipient.getNickname()),
                targetType,
                scheduleId
            ))
            .toList();

        notificationRepository.saveAll(notifications);
    }

    /**
     * 프로젝트 초대 알림을 생성한다.
     * 클릭 대상은 프로젝트 상세 화면이다.
     *
     * @deprecated 알림 문구는 알림 도메인에서 관리하므로 projectTitle을 받는 메서드를 사용한다.
     */
    @Deprecated
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
            .title(createFallbackTitle(NotificationType.PROJECT_INVITED))
            .content(content)
            .targetType(NotificationTargetType.PROJECT)
            .targetId(projectId)
            .build());
    }

    /**
     * 프로젝트 초대 알림 문구를 정책에 맞춰 생성한다.
     */
    @Transactional
    public void createProjectInvitationNotification(
        Long recipientId,
        Long projectId,
        String projectTitle,
        String inviterName
    ) {
        validateRequiredId(projectId);
        validateRequiredId(recipientId);
        validateRequiredText(projectTitle);
        validateRequiredText(inviterName);

        createNotification(NotificationCreateCommand.builder()
            .recipientIds(List.of(recipientId))
            .projectId(projectId)
            .type(NotificationType.PROJECT_INVITED)
            .title(createProjectInvitationTitle(projectTitle))
            .content(createProjectInvitationContent(projectTitle, inviterName))
            .targetType(NotificationTargetType.PROJECT)
            .targetId(projectId)
            .build());
    }

    /**
     * 프로젝트 합류 알림을 프로젝트 참여자에게 생성한다.
     * 합류자 본인도 수신 대상에 포함할 수 있다.
     */
    @Transactional
    public void createProjectJoinedNotifications(
        Long projectId,
        String projectTitle,
        String joinerName,
        List<Long> recipientIds
    ) {
        validateRequiredId(projectId);
        validateRequiredText(projectTitle);
        validateRequiredText(joinerName);

        createNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.PROJECT_JOINED)
            .title(createProjectJoinedTitle(projectTitle))
            .content(createProjectJoinedContent(joinerName))
            .targetType(NotificationTargetType.PROJECT)
            .targetId(projectId)
            .build());
    }

    /**
     * 프로젝트에 새 일정이 등록되었을 때 프로젝트 참여자에게 알림을 생성한다.
     */
    @Transactional
    public void createScheduleCreatedNotifications(
        Long projectId,
        Long scheduleId,
        String projectTitle,
        String scheduleTitle,
        String creatorName,
        List<Long> recipientIds,
        Long actorUserId
    ) {
        validateRequiredId(projectId);
        validateRequiredId(scheduleId);
        validateRequiredText(projectTitle);
        validateRequiredText(scheduleTitle);
        validateRequiredText(creatorName);

        createNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.SCHEDULE_CREATED)
            .title(createScheduleCreatedTitle(projectTitle))
            .content(createScheduleCreatedContent(scheduleTitle, creatorName))
            .targetType(NotificationTargetType.SCHEDULE)
            .targetId(scheduleId)
            .excludeUserId(actorUserId)
            .build());
    }

    /**
     * 영상 피드백 댓글 알림을 생성하거나 기존 미읽음 알림을 갱신한다.
     * 동일 영상 기준으로 그룹핑하므로 targetId는 feedbackId가 아니라 videoId를 사용한다.
     *
     * @deprecated 알림 문구는 알림 도메인에서 관리하므로 videoTitle과 commenterName을 받는 메서드를 사용한다.
     */
    @Deprecated
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
            .title(createFallbackTitle(NotificationType.VIDEO_FEEDBACK_COMMENTED))
            .content(content)
            .targetType(NotificationTargetType.VIDEO)
            .targetId(videoId)
            .excludeUserId(actorUserId)
            .build());
    }

    /**
     * 영상 피드백 알림 문구를 정책에 맞춰 생성하고, 동일 영상 기준으로 그룹핑한다.
     */
    @Transactional
    public void createVideoFeedbackCommentedNotifications(
        Long projectId,
        Long videoId,
        String videoTitle,
        String commenterName,
        List<Long> recipientIds,
        Long actorUserId
    ) {
        validateRequiredId(projectId);
        validateRequiredId(videoId);
        validateRequiredText(videoTitle);
        validateRequiredText(commenterName);
        Project project = getProjectOrNull(projectId);

        NotificationCreateCommand command = NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.VIDEO_FEEDBACK_COMMENTED)
            .title(createVideoFeedbackCommentedTitle(getProjectTitle(project)))
            .content(createVideoFeedbackCommentedContent(videoTitle, commenterName, 1))
            .targetType(NotificationTargetType.VIDEO)
            .targetId(videoId)
            .excludeUserId(actorUserId)
            .build();

        createOrUpdateGroupedNotifications(
            command,
            groupCount -> createVideoFeedbackCommentedContent(videoTitle, commenterName, groupCount)
        );
    }

    /**
     * 새로운 지원자 발생 알림을 생성하거나 기존 미읽음 알림을 갱신한다.
     * 동일 공고 기준으로 그룹핑하므로 targetId는 recruitmentId를 사용한다.
     *
     * @deprecated 알림 문구는 알림 도메인에서 관리하므로 projectTitle, recruitmentTitle, applicantName을 받는 메서드를 사용한다.
     */
    @Deprecated
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
            .title(createFallbackTitle(NotificationType.RECRUITMENT_APPLIED))
            .content(content)
            .targetType(NotificationTargetType.RECRUITMENT)
            .targetId(recruitmentId)
            .build());
    }

    /**
     * 지원자 알림 문구를 정책에 맞춰 생성하고, 동일 공고 기준으로 그룹핑한다.
     */
    @Transactional
    public void createRecruitmentAppliedNotification(
        Long recipientId,
        Long recruitmentId,
        String projectTitle,
        String recruitmentTitle,
        String applicantName
    ) {
        validateRequiredId(recipientId);
        validateRequiredId(recruitmentId);
        validateRequiredText(projectTitle);
        validateRequiredText(recruitmentTitle);
        validateRequiredText(applicantName);

        NotificationCreateCommand command = NotificationCreateCommand.builder()
            .recipientIds(List.of(recipientId))
            .type(NotificationType.RECRUITMENT_APPLIED)
            .title(createRecruitmentAppliedTitle(projectTitle))
            .content(createRecruitmentAppliedContent(recruitmentTitle, applicantName, 1))
            .targetType(NotificationTargetType.RECRUITMENT)
            .targetId(recruitmentId)
            .build();

        createOrUpdateGroupedNotifications(
            command,
            groupCount -> createRecruitmentAppliedContent(recruitmentTitle, applicantName, groupCount)
        );
    }

    /**
     * 프로젝트 공지가 등록되었을 때 프로젝트 참여자에게 알림을 생성한다.
     */
    @Transactional
    public void createNoticeCreatedNotifications(
        Long projectId,
        Long noticeId,
        String projectTitle,
        String noticeTitle,
        String creatorName,
        List<Long> recipientIds,
        Long actorUserId
    ) {
        validateRequiredId(projectId);
        validateRequiredId(noticeId);
        validateRequiredText(projectTitle);
        validateRequiredText(noticeTitle);
        validateRequiredText(creatorName);

        createNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.NOTICE_CREATED)
            .title(createNoticeCreatedTitle(projectTitle))
            .content(createNoticeCreatedContent(noticeTitle, creatorName))
            .targetType(NotificationTargetType.NOTICE)
            .targetId(noticeId)
            .excludeUserId(actorUserId)
            .build());
    }

    /**
     * 프로젝트 파일 등록 알림을 생성한다.
     */
    @Transactional
    public void createFileUploadedNotifications(
        Long projectId,
        String projectTitle,
        String fileName,
        String uploaderName,
        List<Long> recipientIds,
        Long actorUserId
    ) {
        validateRequiredId(projectId);
        validateRequiredText(projectTitle);
        validateRequiredText(fileName);
        validateRequiredText(uploaderName);

        createNotifications(NotificationCreateCommand.builder()
            .recipientIds(recipientIds)
            .projectId(projectId)
            .type(NotificationType.FILE_UPLOADED)
            .title(createFileUploadedTitle(projectTitle))
            .content(createFileUploadedContent(fileName, uploaderName))
            .targetType(NotificationTargetType.PROJECT_FILE)
            .targetId(projectId)
            .excludeUserId(actorUserId)
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

    private void validateRequiredText(String text) {
        if (text == null || text.isBlank()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void createOrUpdateGroupedNotification(
        Users recipient,
        Project project,
        NotificationCreateCommand command
    ) {
        createOrUpdateGroupedNotification(
            recipient,
            project,
            command,
            groupCount -> command.getContent()
        );
    }

    private void createOrUpdateGroupedNotification(
        Users recipient,
        Project project,
        NotificationCreateCommand command,
        IntFunction<String> contentFactory
    ) {
        String targetType = getTargetTypeName(command.getTargetType());
        Object groupingLock = getGroupingLock(
            recipient.getId(),
            command.getType(),
            targetType,
            command.getTargetId()
        );

        synchronized (groupingLock) {
            List<Notification> existingNotifications = notificationRepository.findUnreadGroupedNotificationsForUpdate(
                recipient.getId(),
                command.getType(),
                targetType,
                command.getTargetId(),
                PageRequest.of(0, 1)
            );

            if (!existingNotifications.isEmpty()) {
                Notification existingNotification = existingNotifications.get(0);
                String content = contentFactory.apply(existingNotification.getGroupCount() + 1);
                existingNotification.updateGroupedNotification(command.getTitle(), content);
                return;
            }

            String content = contentFactory.apply(1);
            notificationRepository.save(Notification.create(
                recipient,
                project,
                command.getType(),
                command.getTitle(),
                content,
                targetType,
                command.getTargetId()
            ));
        }
    }

    private void createOrUpdateGroupedNotifications(
        NotificationCreateCommand command,
        IntFunction<String> contentFactory
    ) {
        validateCreateCommand(command);
        validateGroupingCommand(command);

        Project project = getProjectOrNull(command.getProjectId());
        List<Long> recipientIds = getRecipientIds(command);
        if (recipientIds.isEmpty()) {
            return;
        }

        recipientIds.stream()
            .map(this::getActiveUser)
            .forEach(recipient -> createOrUpdateGroupedNotification(recipient, project, command, contentFactory));
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
            .title(notification.getTitle())
            .content(notification.getContent())
            .groupCount(notification.getGroupCount())
            .targetType(notification.getTargetType())
            .targetId(notification.getTargetId())
            .isRead(notification.getIsRead())
            .readAt(notification.getReadAt())
            .createdAt(notification.getCreatedAt())
            .build();
    }

    private String createScheduleAssignedContent(String scheduleTitle, String assigneeName) {
        return assigneeName + "님이 [" + scheduleTitle + "] 담당자로 지정했어요";
    }

    private String createScheduleAssignedTitle(String projectTitle) {
        return joinTitle(projectTitle, "일정 담당자 지정");
    }

    private String createProjectInvitationTitle(String projectTitle) {
        return joinTitle(projectTitle, "프로젝트 초대");
    }

    private String createProjectInvitationContent(String projectTitle, String inviterName) {
        return inviterName + "님이 [" + projectTitle + "] 프로젝트에 초대했어요";
    }

    private String createProjectJoinedTitle(String projectTitle) {
        return joinTitle(projectTitle, "합류");
    }

    private String createProjectJoinedContent(String joinerName) {
        return joinerName + "님이 프로젝트에 합류했어요";
    }

    private String createScheduleCreatedTitle(String projectTitle) {
        return joinTitle(projectTitle, "새 일정");
    }

    private String createScheduleCreatedContent(String scheduleTitle, String creatorName) {
        return creatorName + "님이 [" + scheduleTitle + "] 일정을 등록했어요";
    }

    private String createVideoFeedbackCommentedTitle(String projectTitle) {
        return joinTitle(projectTitle, "새로운 피드백");
    }

    private String createVideoFeedbackCommentedContent(
        String videoTitle,
        String commenterName,
        int groupCount
    ) {
        if (groupCount <= 1) {
            return commenterName + "님이 [" + videoTitle + "]에 새로운 피드백을 남겼어요";
        }

        return "[" + videoTitle + "]에 새로운 피드백 " + groupCount + "건이 등록되었어요";
    }

    private String createRecruitmentAppliedTitle(String projectTitle) {
        return joinTitle(projectTitle, "새로운 지원자");
    }

    private String createRecruitmentAppliedContent(
        String recruitmentTitle,
        String applicantName,
        int groupCount
    ) {
        if (groupCount <= 1) {
            return applicantName + "님이 [" + recruitmentTitle + "]에 지원했어요";
        }

        return "[" + recruitmentTitle + "]에 새로운 지원자 " + groupCount + "명이 지원했어요";
    }

    private String createNoticeCreatedTitle(String projectTitle) {
        return joinTitle(projectTitle, "새 공지");
    }

    private String createNoticeCreatedContent(String noticeTitle, String creatorName) {
        return creatorName + "님이 새 공지를 등록했어요: " + noticeTitle;
    }

    private String createFileUploadedTitle(String projectTitle) {
        return joinTitle(projectTitle, "새 파일");
    }

    private String createFileUploadedContent(String fileName, String uploaderName) {
        return uploaderName + "님이 [" + fileName + "] 파일을 등록했어요";
    }

    private String createFallbackTitle(NotificationType type) {
        return switch (type) {
            case SCHEDULE_ASSIGNED -> "일정 담당자 지정";
            case PROJECT_INVITED -> "프로젝트 초대";
            case PROJECT_JOINED -> "합류";
            case VIDEO_FEEDBACK_COMMENTED -> "새로운 피드백";
            case RECRUITMENT_APPLIED -> "새로운 지원자";
            case SCHEDULE_CREATED -> "새 일정";
            case NOTICE_CREATED -> "새 공지";
            case FILE_UPLOADED -> "새 파일";
            case DEADLINE_REMINDER -> "마감 알림";
        };
    }

    private String joinTitle(String prefix, String suffix) {
        if (prefix == null || prefix.isBlank()) {
            return suffix;
        }

        return prefix + " · " + suffix;
    }

    private String getProjectTitle(Project project) {
        return project != null ? project.getTitle() : null;
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
