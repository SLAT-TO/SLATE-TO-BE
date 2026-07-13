package com.slatto.domain.project.service;

import com.slatto.domain.project.converter.ProjectConverter;
import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectResponse;
import com.slatto.domain.project.dto.ProjectUpdateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectUserRole;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.project.repository.ProjectUserRoleRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private static final int FREE_PROJECT_LIMIT = 5;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MEMBER_PREVIEW_LIMIT = 4;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectUserRoleRepository projectUserRoleRepository;
    private final UserRepository userRepository;
    private final ProjectConverter projectConverter;

    @Transactional
    public ProjectResponse createProject(Long ownerUserId, ProjectCreateRequest request) {
        Users ownerUser = getActiveUser(ownerUserId);
        validateProjectCreationQuota(ownerUserId);

        Project project = projectConverter.toProject(ownerUser, request);
        Project savedProject = projectRepository.save(project);

        ProjectMember ownerMember = projectMemberRepository.save(
            ProjectMember.createAdmin(savedProject, ownerUser)
        );
        saveProjectRoles(ownerMember, request.getRoleNames());

        return projectConverter.toResponse(savedProject);
    }

    public ProjectListResponse getProjects(
        Long currentUserId,
        ProjectStatus status,
        Long cursor,
        int size
    ) {
        validateActiveUserExists(currentUserId);

        int pageSize = normalizePageSize(size);
        List<ProjectMember> projectMembers = projectMemberRepository.findJoinedProjectsByCursor(
            currentUserId,
            status,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = projectMembers.size() > pageSize;
        List<ProjectMember> currentPageMembers = projectMembers.stream()
            .limit(pageSize)
            .toList();

        List<ProjectListResponse.ProjectSummary> items = currentPageMembers.stream()
            .map(ProjectMember::getProject)
            .map(project -> projectConverter.toSummary(
                project,
                countActiveMembers(project),
                getMemberPreviewImageUrls(project),
                resolveLastActivityAt(project)
            ))
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).getId()
            : null;

        return projectConverter.toListResponse(items, nextCursor, hasNext);
    }

    public ProjectDetailResponse getProject(Long projectId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        ProjectMember currentMember = getCurrentMemberOrThrow(projectId, currentUserId);

        List<RoleName> myRoles = projectUserRoleRepository.findAllByProjectMemberId(currentMember.getId())
            .stream()
            .map(ProjectUserRole::getRoleName)
            .toList();

        return projectConverter.toDetailResponse(
            project,
            currentMember,
            myRoles,
            countActiveMembers(project)
        );
    }

    @Transactional
    public ProjectResponse updateProject(
        Long projectId,
        Long currentUserId,
        ProjectUpdateRequest request
    ) {
        Project project = getProjectOrThrow(projectId);
        ProjectMember currentMember = getCurrentMemberOrThrow(projectId, currentUserId);
        validateAdmin(currentMember);

        project.updateInfo(
            request.getTitle(),
            request.getType(),
            request.getCustomTypeName(),
            request.getLengthType(),
            request.getDescription(),
            request.getEndDate(),
            request.getClientName(),
            request.getKind()
        );

        if (request.getStatus() != null) {
            project.changeStatus(request.getStatus());
        }

        return projectConverter.toResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        ProjectMember currentMember = getCurrentMemberOrThrow(projectId, currentUserId);
        validateAdmin(currentMember);

        project.delete();
    }

    private Users getActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private void validateActiveUserExists(Long userId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }

    private void validateProjectCreationQuota(Long ownerUserId) {
        long projectCount = projectRepository.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId);
        if (projectCount >= FREE_PROJECT_LIMIT) {
            throw new BaseException(ProjectErrorCode.PROJECT_LIMIT_EXCEEDED);
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember getCurrentMemberOrThrow(Long projectId, Long currentUserId) {
        return projectMemberRepository.findByProjectIdAndUserIdAndLeftAtIsNull(projectId, currentUserId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED));
    }

    private void validateAdmin(ProjectMember projectMember) {
        if (!projectMember.isAdmin()) {
            throw new BaseException(ProjectErrorCode.PROJECT_ADMIN_REQUIRED);
        }
    }

    private void saveProjectRoles(ProjectMember projectMember, List<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }

        List<ProjectUserRole> projectUserRoles = roleNames.stream()
            .map(roleName -> ProjectUserRole.create(projectMember, roleName))
            .toList();

        projectUserRoleRepository.saveAll(projectUserRoles);
    }

    private long countActiveMembers(Project project) {
        return projectMemberRepository.countByProjectIdAndLeftAtIsNull(project.getId());
    }

    private List<String> getMemberPreviewImageUrls(Project project) {
        return projectMemberRepository.findActiveMembersByProjectId(
                project.getId(),
                PageRequest.of(0, MEMBER_PREVIEW_LIMIT)
            )
            .stream()
            .map(ProjectMember::getUser)
            .map(Users::getProfileImageUrl)
            .filter(Objects::nonNull)
            .toList();
    }

    private LocalDateTime resolveLastActivityAt(Project project) {
        return project.getUpdatedAt() != null ? project.getUpdatedAt() : project.getCreatedAt();
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
