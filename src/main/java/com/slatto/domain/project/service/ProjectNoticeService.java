package com.slatto.domain.project.service;

import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.project.dto.ProjectNoticeCreateRequest;
import com.slatto.domain.project.dto.ProjectNoticeListResponse;
import com.slatto.domain.project.dto.ProjectNoticeReadResponse;
import com.slatto.domain.project.dto.ProjectNoticeResponse;
import com.slatto.domain.project.dto.ProjectNoticeUpdateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectNotice;
import com.slatto.domain.project.entity.ProjectNoticeRead;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectNoticeReadRepository;
import com.slatto.domain.project.repository.ProjectNoticeRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ProjectNoticeRepository projectNoticeRepository;
    private final ProjectNoticeReadRepository projectNoticeReadRepository;
    private final ProjectAccessValidator projectAccessValidator;
    private final ActivityLogService activityLogService;

    public ProjectNoticeListResponse getProjectNotices(
        Long projectId,
        Long currentUserId,
        Long cursor,
        int size
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        int pageSize = normalizePageSize(size);
        List<ProjectNotice> projectNotices = projectNoticeRepository.findActiveNoticesByCursor(
            projectId,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = projectNotices.size() > pageSize;
        List<ProjectNotice> currentPageNotices = projectNotices.stream()
            .limit(pageSize)
            .toList();
        Map<Long, Boolean> readByNoticeId = getReadByNoticeId(currentUserId, currentPageNotices);

        List<ProjectNoticeResponse> items = currentPageNotices.stream()
            .map(projectNotice -> toResponse(
                projectNotice,
                readByNoticeId.getOrDefault(projectNotice.getId(), false)
            ))
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).getId()
            : null;

        return ProjectNoticeListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    @Transactional
    public ProjectNoticeResponse createProjectNotice(
        Long projectId,
        Long currentUserId,
        ProjectNoticeCreateRequest request
    ) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        ProjectNotice projectNotice = ProjectNotice.create(
            project,
            currentMember.getUser(),
            request.getTitle(),
            request.getContent()
        );
        ProjectNotice savedNotice = projectNoticeRepository.save(projectNotice);
        activityLogService.createNoticeCreatedLog(projectId, currentUserId, savedNotice.getId());

        return toResponse(savedNotice, false);
    }

    @Transactional
    public ProjectNoticeResponse updateProjectNotice(
        Long projectId,
        Long noticeId,
        Long currentUserId,
        ProjectNoticeUpdateRequest request
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectNotice projectNotice = getActiveNoticeOrThrow(projectId, noticeId);
        validateNoticeEditable(projectNotice, currentMember, currentUserId);

        projectNotice.update(request.getTitle(), request.getContent());
        boolean isRead = projectNoticeReadRepository.findByNoticeIdAndUserId(noticeId, currentUserId)
            .isPresent();

        return toResponse(projectNotice, isRead);
    }

    @Transactional
    public void deleteProjectNotice(Long projectId, Long noticeId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectNotice projectNotice = getActiveNoticeOrThrow(projectId, noticeId);
        validateNoticeEditable(projectNotice, currentMember, currentUserId);

        projectNotice.delete();
    }

    @Transactional
    public ProjectNoticeReadResponse readProjectNotice(Long projectId, Long noticeId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        getActiveNoticeOrThrow(projectId, noticeId);

        projectNoticeReadRepository.upsertRead(noticeId, currentUserId);
        ProjectNoticeRead projectNoticeRead = projectNoticeReadRepository
            .findByNoticeIdAndUserId(noticeId, currentUserId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR));

        return toReadResponse(projectNoticeRead);
    }

    private ProjectNotice getActiveNoticeOrThrow(Long projectId, Long noticeId) {
        return projectNoticeRepository.findActiveNoticeByProjectIdAndNoticeId(projectId, noticeId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_NOTICE_NOT_FOUND));
    }

    private void validateNoticeEditable(
        ProjectNotice projectNotice,
        ProjectMember currentMember,
        Long currentUserId
    ) {
        if (projectNotice.isWrittenBy(currentUserId) || currentMember.isAdmin()) {
            return;
        }

        throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    private ProjectNoticeResponse toResponse(ProjectNotice projectNotice, boolean isRead) {
        Users writer = projectNotice.getWriter();

        return ProjectNoticeResponse.builder()
            .id(projectNotice.getId())
            .title(projectNotice.getTitle())
            .content(projectNotice.getContent())
            .writer(ProjectNoticeResponse.WriterSummary.builder()
                .id(writer.getId())
                .nickname(writer.getNickname())
                .build())
            .isRead(isRead)
            .createdAt(projectNotice.getCreatedAt())
            .updatedAt(projectNotice.getUpdatedAt())
            .build();
    }

    private ProjectNoticeReadResponse toReadResponse(ProjectNoticeRead projectNoticeRead) {
        return ProjectNoticeReadResponse.builder()
            .id(projectNoticeRead.getNotice().getId())
            .isRead(true)
            .readAt(projectNoticeRead.getReadAt())
            .build();
    }

    private Map<Long, Boolean> getReadByNoticeId(Long userId, List<ProjectNotice> projectNotices) {
        List<Long> noticeIds = projectNotices.stream()
            .map(ProjectNotice::getId)
            .toList();

        if (noticeIds.isEmpty()) {
            return Map.of();
        }

        return projectNoticeReadRepository.findAllByUserIdAndNoticeIds(userId, noticeIds)
            .stream()
            .collect(Collectors.toMap(
                projectNoticeRead -> projectNoticeRead.getNotice().getId(),
                projectNoticeRead -> true,
                (left, right) -> left
            ));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
