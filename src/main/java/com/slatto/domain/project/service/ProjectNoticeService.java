package com.slatto.domain.project.service;

import com.slatto.domain.project.dto.ProjectNoticeCreateRequest;
import com.slatto.domain.project.dto.ProjectNoticeListResponse;
import com.slatto.domain.project.dto.ProjectNoticeResponse;
import com.slatto.domain.project.dto.ProjectNoticeUpdateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectNotice;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectNoticeRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ProjectNoticeRepository projectNoticeRepository;
    private final ProjectAccessValidator projectAccessValidator;

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

        List<ProjectNoticeResponse> items = currentPageNotices.stream()
            .map(this::toResponse)
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

        return toResponse(savedNotice);
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

        return toResponse(projectNotice);
    }

    @Transactional
    public void deleteProjectNotice(Long projectId, Long noticeId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectNotice projectNotice = getActiveNoticeOrThrow(projectId, noticeId);
        validateNoticeEditable(projectNotice, currentMember, currentUserId);

        projectNotice.delete();
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

    private ProjectNoticeResponse toResponse(ProjectNotice projectNotice) {
        Users writer = projectNotice.getWriter();

        return ProjectNoticeResponse.builder()
            .id(projectNotice.getId())
            .title(projectNotice.getTitle())
            .content(projectNotice.getContent())
            .writer(ProjectNoticeResponse.WriterSummary.builder()
                .id(writer.getId())
                .nickname(writer.getNickname())
                .build())
            .createdAt(projectNotice.getCreatedAt())
            .updatedAt(projectNotice.getUpdatedAt())
            .build();
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
