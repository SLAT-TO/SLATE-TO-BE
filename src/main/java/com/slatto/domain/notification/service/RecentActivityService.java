package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.ActivityLogCursor;
import com.slatto.domain.notification.dto.ActivityLogListResponse;
import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadCommandRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadRepository;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentActivityService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ActivityLogRepository activityLogRepository;
    private final ProjectActivityReadRepository projectActivityReadRepository;
    private final ProjectActivityReadCommandRepository projectActivityReadCommandRepository;
    private final ProjectAccessValidator projectAccessValidator;

    public ActivityLogListResponse getRecentActivities(
        Long projectId,
        Long currentUserId,
        String cursor,
        int size
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        int pageSize = normalizePageSize(size);
        ActivityLogCursor activityCursor = parseCursor(cursor);
        List<ActivityLog> fetchedActivities = activityLogRepository.findRecentActivitiesByCursor(
            projectId,
            activityCursor != null ? activityCursor.createdAt() : null,
            activityCursor != null ? activityCursor.activityId() : null,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = fetchedActivities.size() > pageSize;
        List<ActivityLog> currentPageActivities = fetchedActivities.stream()
            .limit(pageSize)
            .toList();

        Set<Long> readActivityLogIds = findReadActivityLogIds(currentMember.getId(), currentPageActivities);
        List<ActivityLogListResponse.ActivityLogItem> items = currentPageActivities.stream()
            .map(activityLog -> toItem(activityLog, readActivityLogIds))
            .toList();

        String nextCursor = hasNext && !currentPageActivities.isEmpty()
            ? new ActivityLogCursor(
                currentPageActivities.getLast().getCreatedAt(),
                currentPageActivities.getLast().getId()
            ).toValue()
            : null;

        return ActivityLogListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    @Transactional
    public void markActivityAsRead(Long projectId, Long activityId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ActivityLog activityLog = activityLogRepository.findByIdAndProjectId(activityId, projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        projectActivityReadCommandRepository.insertIfAbsent(
            currentMember.getId(),
            activityLog.getId()
        );
    }

    @Transactional
    public void markAllActivitiesAsRead(Long projectId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        projectActivityReadCommandRepository.insertAllIfAbsentByProjectId(currentMember.getId(), projectId);
    }

    private ActivityLogListResponse.ActivityLogItem toItem(
        ActivityLog activityLog,
        Set<Long> readActivityLogIds
    ) {
        return ActivityLogListResponse.ActivityLogItem.builder()
            .activityId(activityLog.getId())
            .type(activityLog.getType())
            .content(activityLog.getContent())
            .targetType(activityLog.getTargetType())
            .targetId(activityLog.getTargetId())
            .createdAt(activityLog.getCreatedAt())
            .isNew(!readActivityLogIds.contains(activityLog.getId()))
            .build();
    }

    private Set<Long> findReadActivityLogIds(Long projectMemberId, Collection<ActivityLog> activityLogs) {
        if (activityLogs.isEmpty()) {
            return Set.of();
        }

        return projectActivityReadRepository.findReadActivityLogIds(
            projectMemberId,
            activityLogs.stream().map(ActivityLog::getId).toList()
        );
    }

    private ActivityLogCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        String[] values = cursor.split("_", -1);
        if (values.length != 2) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            return new ActivityLogCursor(
                LocalDateTime.parse(values[0]),
                Long.parseLong(values[1])
            );
        } catch (RuntimeException exception) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
