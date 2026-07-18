package com.slatto.domain.project.service;

import com.slatto.domain.project.dto.ProjectMemberDetailResponse;
import com.slatto.domain.project.dto.ProjectMemberListResponse;
import com.slatto.domain.project.dto.ProjectMemberUpdateRequest;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectUserRole;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectUserRoleRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectUserRoleRepository projectUserRoleRepository;
    private final ProjectAccessValidator projectAccessValidator;

    public ProjectMemberListResponse getProjectMembers(Long projectId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        List<ProjectMember> projectMembers = projectMemberRepository.findAllActiveMembersByProjectId(projectId);
        Map<Long, List<RoleName>> roleNamesByMemberId = getRoleNamesByMemberId(projectMembers);

        List<ProjectMemberListResponse.MemberSummary> items = projectMembers.stream()
            .map(projectMember -> toMemberSummary(
                projectMember,
                roleNamesByMemberId.getOrDefault(projectMember.getId(), List.of())
            ))
            .toList();

        return ProjectMemberListResponse.builder()
            .items(items)
            .memberCount((long) items.size())
            .build();
    }

    public ProjectMemberDetailResponse getProjectMember(
        Long projectId,
        Long memberId,
        Long currentUserId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        ProjectMember projectMember = projectMemberRepository.findActiveMemberByProjectIdAndMemberId(
                projectId,
                memberId
            )
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));

        List<RoleName> roleNames = projectUserRoleRepository.findAllByProjectMemberIdOrderByIdAsc(memberId)
            .stream()
            .map(ProjectUserRole::getRoleName)
            .toList();

        return toMemberDetailResponse(projectMember, roleNames);
    }

    @Transactional
    public ProjectMemberDetailResponse updateProjectMemberRoles(
        Long projectId,
        Long memberId,
        Long currentUserId,
        ProjectMemberUpdateRequest request
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentAdminOrThrow(projectId, currentUserId);

        ProjectMember projectMember = projectMemberRepository.findActiveMemberByProjectIdAndMemberId(
                projectId,
                memberId
            )
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));

        List<RoleName> roleNames = request.getRoleNames()
            .stream()
            .distinct()
            .toList();

        replaceProjectMemberRoles(projectMember, roleNames);

        return toMemberDetailResponse(projectMember, roleNames);
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long memberId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentAdminOrThrow(projectId, currentUserId);

        ProjectMember projectMember = projectMemberRepository.findActiveMemberByProjectIdAndMemberId(
                projectId,
                memberId
            )
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));

        if (projectMember.isMemberOf(currentUserId)) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        projectMember.leave();
    }

    private Map<Long, List<RoleName>> getRoleNamesByMemberId(List<ProjectMember> projectMembers) {
        List<Long> projectMemberIds = projectMembers.stream()
            .map(ProjectMember::getId)
            .toList();

        if (projectMemberIds.isEmpty()) {
            return Map.of();
        }

        return projectUserRoleRepository
            .findAllByProjectMemberIdsOrderByProjectMemberIdAscAndIdAsc(projectMemberIds)
            .stream()
            .collect(Collectors.groupingBy(
                projectUserRole -> projectUserRole.getProjectMember().getId(),
                Collectors.mapping(ProjectUserRole::getRoleName, Collectors.toList())
            ));
    }

    private void replaceProjectMemberRoles(ProjectMember projectMember, List<RoleName> roleNames) {
        List<ProjectUserRole> currentRoles = projectUserRoleRepository.findAllByProjectMemberId(projectMember.getId());
        projectUserRoleRepository.deleteAll(currentRoles);

        List<ProjectUserRole> newRoles = roleNames.stream()
            .map(roleName -> ProjectUserRole.create(projectMember, roleName))
            .toList();

        projectUserRoleRepository.saveAll(newRoles);
    }

    private ProjectMemberListResponse.MemberSummary toMemberSummary(
        ProjectMember projectMember,
        List<RoleName> roleNames
    ) {
        Users user = projectMember.getUser();

        return ProjectMemberListResponse.MemberSummary.builder()
            .memberId(projectMember.getId())
            .userId(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .permission(projectMember.getPermission())
            .roleNames(roleNames)
            .joinedAt(projectMember.getJoinedAt())
            .build();
    }

    private ProjectMemberDetailResponse toMemberDetailResponse(
        ProjectMember projectMember,
        List<RoleName> roleNames
    ) {
        Users user = projectMember.getUser();

        return ProjectMemberDetailResponse.builder()
            .memberId(projectMember.getId())
            .userId(user.getId())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .permission(projectMember.getPermission())
            .roleNames(roleNames)
            .joinedAt(projectMember.getJoinedAt())
            .build();
    }
}
