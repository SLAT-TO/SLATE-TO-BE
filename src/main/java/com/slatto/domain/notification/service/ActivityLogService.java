package com.slatto.domain.notification.service;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.enums.ActorType;
import com.slatto.domain.notification.enums.NotificationTargetType;
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

    @Transactional
    private void createUserActivityLog(
        Long projectId,
        Long actorUserId,
        ActivityLogType type,
        String content,
        NotificationTargetType targetType,
        Long targetId
    ) {
        Project project = getActiveProject(projectId);
        Users actor = getActiveUser(actorUserId);

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

    private String getTargetTypeName(NotificationTargetType targetType) {
        return targetType != null ? targetType.name() : null;
    }
}
