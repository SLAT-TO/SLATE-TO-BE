package com.slatto.domain.project.service;

import com.slatto.domain.project.converter.ProjectConverter;
import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectPinResponse;
import com.slatto.domain.project.dto.ProjectResponse;
import com.slatto.domain.project.dto.ProjectUpdateRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectPin;
import com.slatto.domain.project.entity.ProjectUserRole;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectPinRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.project.repository.ProjectUserRoleRepository;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.notification.repository.ProjectLatestActivityProjection;
import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.user.dto.ProjectPortfolioCreateCommand;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.service.PortfolioService;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final ProjectPinRepository projectPinRepository;
    private final ProjectUserRoleRepository projectUserRoleRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final ProjectConverter projectConverter;
    private final ProjectAccessValidator projectAccessValidator;
    private final PortfolioService portfolioService;
    private final ActivityLogService activityLogService;
    private final ActivityLogRepository activityLogRepository;

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
        LocalDateTime cursorPinnedAt = getProjectCursorPinnedAt(currentUserId, cursor);
        List<ProjectMember> projectMembers = projectMemberRepository.findJoinedProjectsByCursor(
            currentUserId,
            status,
            cursor,
            cursorPinnedAt,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = projectMembers.size() > pageSize;
        List<ProjectMember> currentPageMembers = projectMembers.stream()
            .limit(pageSize)
            .toList();

        Map<Long, List<RoleName>> roleNamesByMemberId = getRoleNamesByMemberId(currentPageMembers);
        Map<Long, String> previewImageUrlByProjectId = getPreviewImageUrlByProjectId(currentPageMembers);
        Map<Long, LocalDateTime> pinnedAtByProjectId = getPinnedAtByProjectId(currentUserId, currentPageMembers);
        Map<Long, LocalDateTime> lastActivityAtByProjectId = getLastActivityAtByProjectId(currentPageMembers);

        Long nextCursor = hasNext && !currentPageMembers.isEmpty()
            ? currentPageMembers.get(currentPageMembers.size() - 1).getProject().getId()
            : null;

        List<ProjectListResponse.ProjectSummary> items = currentPageMembers.stream()
            .map(projectMember -> projectConverter.toSummary(
                projectMember.getProject(),
                countActiveMembers(projectMember.getProject()),
                getMemberPreviewImageUrls(projectMember.getProject()),
                roleNamesByMemberId.getOrDefault(projectMember.getId(), List.of()),
                previewImageUrlByProjectId.get(projectMember.getProject().getId()),
                pinnedAtByProjectId.get(projectMember.getProject().getId()),
                projectMember.getPermission(),
                lastActivityAtByProjectId.get(projectMember.getProject().getId())
            ))
            .toList();

        return projectConverter.toListResponse(items, nextCursor, hasNext);
    }

    public ProjectDetailResponse getProject(Long projectId, Long currentUserId) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        List<RoleName> roleNames = projectUserRoleRepository.findAllByProjectMemberId(currentMember.getId())
            .stream()
            .map(ProjectUserRole::getRoleName)
            .toList();
        ProjectPin projectPin = projectPinRepository.findByUserIdAndProjectId(currentUserId, projectId)
            .orElse(null);

        return projectConverter.toDetailResponse(
            project,
            currentMember,
            roleNames,
            projectPin,
            countActiveMembers(project)
        );
    }

    @Transactional
    public ProjectPinResponse pinProject(Long projectId, Long currentUserId) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        projectPinRepository.insertIgnore(currentUserId, projectId, LocalDateTime.now());
        ProjectPin projectPin = projectPinRepository.findByUserIdAndProjectId(currentUserId, projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR));

        return projectConverter.toPinResponse(project.getId(), projectPin.getPinnedAt());
    }

    @Transactional
    public ProjectPinResponse unpinProject(Long projectId, Long currentUserId) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        projectPinRepository.findByUserIdAndProjectId(currentUserId, projectId)
            .ifPresent(projectPinRepository::delete);

        return projectConverter.toPinResponse(project.getId(), null);
    }

    @Transactional
    public ProjectResponse updateProject(
        Long projectId,
        Long currentUserId,
        ProjectUpdateRequest request
    ) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentAdminOrThrow(projectId, currentUserId);
        ProjectStatus previousStatus = project.getStatus();

        // 완료는 최종 상태다. 참여자 포트폴리오가 이미 만들어졌기 때문에 되돌리면
        // 다시 완료할 때 같은 이력이 두 번 생긴다.
        if (previousStatus == ProjectStatus.COMPLETED
            && request.getStatus() != null
            && request.getStatus() != ProjectStatus.COMPLETED) {
            throw new BaseException(ProjectErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        project.updateInfo(
            request.getTitle(),
            request.getType(),
            request.getLengthType(),
            request.getDescription(),
            request.getEndDate(),
            request.getClientName(),
            request.getKind()
        );

        // 같은 요청에서 바뀐 값이 포트폴리오로 옮겨가야 하므로 updateInfo 다음에 처리한다.
        if (request.getStatus() == ProjectStatus.COMPLETED && previousStatus != ProjectStatus.COMPLETED) {
            completeProject(project);
        } else if (request.getStatus() != null) {
            project.changeStatus(request.getStatus());
        }

        activityLogService.createProjectUpdatedLog(projectId, currentUserId);

        if (request.getStatus() != null && previousStatus != request.getStatus()) {
            activityLogService.createProjectStatusChangedLog(
                projectId,
                currentUserId,
                previousStatus.getLabel(),
                request.getStatus().getLabel()
            );
        }

        return projectConverter.toResponse(project);
    }

    // 완료 전환과 포트폴리오 생성을 한 트랜잭션에서 처리한다.
    // 포트폴리오 생성이 실패하면 완료 전환도 함께 롤백되어야 한다.
    private void completeProject(Project project) {
        // 포트폴리오의 title 은 NOT NULL 이라 제목이 비면 저장이 DB 제약으로 끊긴다.
        // 생성·수정 요청 모두 @NotBlank 라 API 로는 비어질 수 없지만, 그 밖의 경로로 들어온
        // 값까지 500 으로 나가지 않도록 여기서 막는다.
        // kind 는 선택 입력이므로 비어 있어도 완료할 수 있다.
        if (!StringUtils.hasText(project.getTitle())) {
            throw new BaseException(ProjectErrorCode.PROJECT_TITLE_REQUIRED);
        }

        if (projectRepository.markCompleted(project.getId()) == 0) {
            throw new BaseException(ProjectErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        // 벌크 UPDATE 는 영속성 컨텍스트를 거치지 않는다. 메모리 상태를 맞추지 않으면
        // 커밋 시점의 더티 체킹 UPDATE 가 예전 status 로 덮어쓴다.
        project.changeStatus(ProjectStatus.COMPLETED);

        portfolioService.createProjectPortfolios(toPortfolioCommand(project));
    }

    private ProjectPortfolioCreateCommand toPortfolioCommand(Project project) {
        // 나간 멤버와 탈퇴한 유저는 이력을 받지 않는다.
        List<ProjectMember> members = projectMemberRepository
            .findAllActiveMembersByProjectId(project.getId())
            .stream()
            .filter(member -> member.getUser().getDeletedAt() == null)
            .toList();

        Map<Long, List<RoleName>> roleNamesByMemberId = findRoleNamesByMemberIds(
            members.stream().map(ProjectMember::getId).toList()
        );

        List<ProjectPortfolioCreateCommand.Participant> participants = members.stream()
            .map(member -> ProjectPortfolioCreateCommand.Participant.builder()
                .user(member.getUser())
                .roles(roleNamesByMemberId.getOrDefault(member.getId(), List.of()))
                .build())
            .toList();

        return ProjectPortfolioCreateCommand.builder()
            .title(project.getTitle())
            .type(project.getType())
            .kind(project.getKind())
            .clientName(project.getClientName())
            .description(project.getDescription())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .participants(participants)
            .build();
    }

    private Map<Long, List<RoleName>> findRoleNamesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return projectUserRoleRepository.findAllByProjectMemberIdIn(memberIds)
            .stream()
            .collect(Collectors.groupingBy(
                role -> role.getProjectMember().getId(),
                Collectors.mapping(ProjectUserRole::getRoleName, Collectors.toList())
            ));
    }

    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.getCurrentAdminOrThrow(projectId, currentUserId);

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

    private Map<Long, String> getPreviewImageUrlByProjectId(List<ProjectMember> projectMembers) {
        List<Long> projectIds = projectMembers.stream()
            .map(ProjectMember::getProject)
            .map(Project::getId)
            .toList();

        if (projectIds.isEmpty()) {
            return Map.of();
        }

        return videoRepository.findLatestThumbnailUrlsByProjectIds(projectIds);
    }

    private Map<Long, LocalDateTime> getPinnedAtByProjectId(Long userId, List<ProjectMember> projectMembers) {
        List<Long> projectIds = projectMembers.stream()
            .map(ProjectMember::getProject)
            .map(Project::getId)
            .toList();

        if (projectIds.isEmpty()) {
            return Map.of();
        }

        return projectPinRepository.findAllByUserIdAndProjectIds(userId, projectIds)
            .stream()
            .collect(Collectors.toMap(
                projectPin -> projectPin.getProject().getId(),
                ProjectPin::getPinnedAt
            ));
    }

    private Map<Long, LocalDateTime> getLastActivityAtByProjectId(List<ProjectMember> projectMembers) {
        List<Long> projectIds = projectMembers.stream()
            .map(ProjectMember::getProject)
            .map(Project::getId)
            .toList();

        if (projectIds.isEmpty()) {
            return Map.of();
        }

        return activityLogRepository.findLatestActivityAtByProjectIds(projectIds)
            .stream()
            .collect(Collectors.toMap(
                ProjectLatestActivityProjection::getProjectId,
                ProjectLatestActivityProjection::getLastActivityAt
            ));
    }

    private LocalDateTime getProjectCursorPinnedAt(Long userId, Long cursor) {
        if (cursor == null) {
            return null;
        }

        return projectPinRepository.findByUserIdAndProjectId(userId, cursor)
            .map(ProjectPin::getPinnedAt)
            .orElse(null);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
