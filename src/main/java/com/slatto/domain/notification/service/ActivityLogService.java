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
    private final ActivityMessageFactory activityMessageFactory;

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
            activityMessageFactory.projectMemberJoined(actor.getNickname()),
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
            activityMessageFactory.projectStatusChanged(actor.getNickname(), previousStatus, changedStatus),
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
            activityMessageFactory.projectUpdated(actor.getNickname()),
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
            activityMessageFactory.scheduleCreated(actor.getNickname(), scheduleTitle),
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
            activityMessageFactory.scheduleUpdated(actor.getNickname(), scheduleTitle),
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
            activityMessageFactory.noticeCreated(actor.getNickname()),
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
            activityMessageFactory.fileUploaded(actor.getNickname(), fileName),
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
            activityMessageFactory.videoFeedbackCommented(actor.getNickname(), videoTitle),
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
