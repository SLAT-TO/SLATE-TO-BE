package com.slatto.domain.notification.service;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.enums.ActorType;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * 프로젝트에 새 참여자가 합류했을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createProjectMemberJoinedLog(Long projectId, Long actorUserId) {
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.PROJECT_MEMBER_JOINED,
            createProjectMemberJoinedContent(actor.getNickname()),
            ActivityLogTargetType.PROJECT,
            projectId
        );
    }

    /**
     * 프로젝트 진행 단계가 변경되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createProjectStatusChangedLog(
        Long projectId,
        Long actorUserId,
        String previousStatus,
        String changedStatus
    ) {
        validateRequiredText(previousStatus);
        validateRequiredText(changedStatus);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.PROJECT_STATUS_CHANGED,
            createProjectStatusChangedContent(actor.getNickname(), previousStatus, changedStatus),
            ActivityLogTargetType.PROJECT,
            projectId
        );
    }

    /**
     * 프로젝트 기본 정보가 수정되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createProjectUpdatedLog(Long projectId, Long actorUserId) {
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.PROJECT_UPDATED,
            createProjectUpdatedContent(actor.getNickname()),
            ActivityLogTargetType.PROJECT,
            projectId
        );
    }

    /**
     * 프로젝트 일정이 등록되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createScheduleCreatedLog(
        Long projectId,
        Long actorUserId,
        Long scheduleId,
        String scheduleTitle
    ) {
        validateRequiredId(scheduleId);
        validateRequiredText(scheduleTitle);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.SCHEDULE_CREATED,
            createScheduleCreatedContent(actor.getNickname(), scheduleTitle),
            ActivityLogTargetType.SCHEDULE,
            scheduleId
        );
    }

    /**
     * 프로젝트 일정이 수정되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createScheduleUpdatedLog(
        Long projectId,
        Long actorUserId,
        Long scheduleId,
        String scheduleTitle
    ) {
        validateRequiredId(scheduleId);
        validateRequiredText(scheduleTitle);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.SCHEDULE_UPDATED,
            createScheduleUpdatedContent(actor.getNickname(), scheduleTitle),
            ActivityLogTargetType.SCHEDULE,
            scheduleId
        );
    }

    /**
     * 프로젝트 공지가 등록되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createNoticeCreatedLog(
        Long projectId,
        Long actorUserId,
        Long noticeId
    ) {
        validateRequiredId(noticeId);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.NOTICE_CREATED,
            createNoticeCreatedContent(actor.getNickname()),
            ActivityLogTargetType.NOTICE,
            noticeId
        );
    }

    /**
     * 프로젝트 파일이 등록되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createFileUploadedLog(
        Long projectId,
        Long actorUserId,
        Long fileId,
        String fileName
    ) {
        validateRequiredId(fileId);
        validateRequiredText(fileName);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.FILE_UPLOADED,
            createFileUploadedContent(actor.getNickname(), fileName),
            ActivityLogTargetType.FILE,
            fileId
        );
    }

    /**
     * 영상에 피드백 댓글이 등록되었을 때 최근활동을 저장한다.
     */
    @Transactional
    public void createVideoFeedbackCommentedLog(
        Long projectId,
        Long actorUserId,
        Long videoId,
        String videoTitle
    ) {
        validateRequiredId(videoId);
        validateRequiredText(videoTitle);
        Users actor = getActiveUser(actorUserId);

        createUserActivityLog(
            projectId,
            actor,
            ActivityLogType.VIDEO_FEEDBACK_COMMENTED,
            createVideoFeedbackCommentedContent(actor.getNickname(), videoTitle),
            ActivityLogTargetType.VIDEO,
            videoId
        );
    }

    private void createUserActivityLog(
        Long projectId,
        Users actor,
        ActivityLogType type,
        String content,
        ActivityLogTargetType targetType,
        Long targetId
    ) {
        Project project = getActiveProject(projectId);

        activityLogRepository.save(ActivityLog.create(
            project,
            actor.getId(),
            ActorType.USER,
            type,
            content,
            getTargetTypeName(targetType),
            targetId
        ));
    }

    private String createProjectMemberJoinedContent(String actorName) {
        return actorName + "님이 프로젝트에 합류했어요";
    }

    private String createProjectStatusChangedContent(
        String actorName,
        String previousStatus,
        String changedStatus
    ) {
        return actorName + "님이 프로젝트 단계를 '" + previousStatus + "'에서 '" + changedStatus + "'으로 변경했어요";
    }

    private String createProjectUpdatedContent(String actorName) {
        return actorName + "님이 프로젝트 정보를 수정했어요";
    }

    private String createScheduleCreatedContent(String actorName, String scheduleTitle) {
        return actorName + "님이 [" + scheduleTitle + "] 일정을 등록했어요";
    }

    private String createScheduleUpdatedContent(String actorName, String scheduleTitle) {
        return actorName + "님이 [" + scheduleTitle + "] 일정을 수정했어요";
    }

    private String createNoticeCreatedContent(String actorName) {
        return actorName + "님이 새 공지를 등록했어요";
    }

    private String createFileUploadedContent(String actorName, String fileName) {
        return actorName + "님이 [" + fileName + "] 파일을 등록했어요";
    }

    private String createVideoFeedbackCommentedContent(String actorName, String videoTitle) {
        return actorName + "님이 [" + videoTitle + "]에 피드백을 남겼어요";
    }

    private Project getActiveProject(Long projectId) {
        validateRequiredId(projectId);

        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private Users getActiveUser(Long userId) {
        validateRequiredId(userId);

        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
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

    private String getTargetTypeName(ActivityLogTargetType targetType) {
        return targetType != null ? targetType.name() : null;
    }
}
