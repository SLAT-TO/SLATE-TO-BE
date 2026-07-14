package com.slatto.domain.project.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAccessValidator {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    public ProjectMember getCurrentMemberOrThrow(Long projectId, Long currentUserId) {
        return projectMemberRepository.findByProjectIdAndUserIdAndLeftAtIsNull(projectId, currentUserId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED));
    }

    public ProjectMember getCurrentAdminOrThrow(Long projectId, Long currentUserId) {
        ProjectMember currentMember = getCurrentMemberOrThrow(projectId, currentUserId);
        validateAdmin(currentMember);

        return currentMember;
    }

    public void validateProjectAccess(Long projectId, Long currentUserId) {
        getCurrentMemberOrThrow(projectId, currentUserId);
    }

    public void validateAdmin(ProjectMember projectMember) {
        if (!projectMember.isAdmin()) {
            throw new BaseException(ProjectErrorCode.PROJECT_ADMIN_REQUIRED);
        }
    }
}
