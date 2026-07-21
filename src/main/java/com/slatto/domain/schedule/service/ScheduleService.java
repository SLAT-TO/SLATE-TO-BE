package com.slatto.domain.schedule.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.entity.ProjectUserRole;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectUserRoleRepository;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.schedule.converter.ScheduleConverter;
import com.slatto.domain.schedule.dto.ScheduleCalendarResponse;
import com.slatto.domain.schedule.dto.ScheduleCreateRequest;
import com.slatto.domain.schedule.dto.ScheduleDailyResponse;
import com.slatto.domain.schedule.dto.ScheduleParticipantCandidateResponse;
import com.slatto.domain.schedule.dto.ScheduleResponse;
import com.slatto.domain.schedule.dto.ScheduleUpdateRequest;
import com.slatto.domain.schedule.entity.Schedule;
import com.slatto.domain.schedule.entity.ScheduleParticipant;
import com.slatto.domain.schedule.entity.SchedulePrivateMemo;
import com.slatto.domain.schedule.enums.ScheduleQueryScope;
import com.slatto.domain.schedule.enums.ScheduleScope;
import com.slatto.domain.schedule.repository.ScheduleParticipantRepository;
import com.slatto.domain.schedule.repository.SchedulePrivateMemoRepository;
import com.slatto.domain.schedule.repository.ScheduleRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final SchedulePrivateMemoRepository schedulePrivateMemoRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectUserRoleRepository projectUserRoleRepository;
    private final ProjectAccessValidator projectAccessValidator;
    private final ScheduleConverter scheduleConverter;

    public ScheduleDailyResponse getDailySchedules(
        Long currentUserId,
        LocalDate date,
        ScheduleQueryScope scope,
        Long projectId
    ) {
        if (date == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        getActiveUser(currentUserId);

        ScheduleQueryScope queryScope = scope != null ? scope : ScheduleQueryScope.ALL;
        validateProjectFilter(currentUserId, queryScope, projectId);

        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();
        List<Schedule> schedules = scheduleRepository.findVisibleSchedulesBetween(
            currentUserId,
            queryScope.toScheduleScope(),
            projectId,
            startAt,
            endAt
        );

        List<Long> scheduleIds = schedules.stream()
            .map(Schedule::getId)
            .toList();
        Map<Long, List<ScheduleParticipant>> participantsByScheduleId =
            getParticipantsByScheduleId(scheduleIds);
        Map<Long, String> privateMemoByScheduleId =
            getPrivateMemoByScheduleId(scheduleIds, currentUserId);

        return scheduleConverter.toDailyResponse(
            date,
            currentUserId,
            schedules,
            participantsByScheduleId,
            privateMemoByScheduleId
        );
    }

    public ScheduleCalendarResponse getCalendarSchedules(
        Long currentUserId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ScheduleQueryScope scope,
        Long projectId
    ) {
        validatePeriod(startAt, endAt);
        getActiveUser(currentUserId);

        ScheduleQueryScope queryScope = scope != null ? scope : ScheduleQueryScope.ALL;
        validateProjectFilter(currentUserId, queryScope, projectId);
        List<Schedule> schedules = scheduleRepository.findVisibleSchedulesBetween(
            currentUserId,
            queryScope.toScheduleScope(),
            projectId,
            startAt,
            endAt
        );

        return scheduleConverter.toCalendarResponse(schedules);
    }

    private Map<Long, List<ScheduleParticipant>> getParticipantsByScheduleId(List<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }

        return scheduleParticipantRepository.findActiveParticipantsByScheduleIds(scheduleIds)
            .stream()
            .collect(Collectors.groupingBy(participant -> participant.getSchedule().getId()));
    }

    private Map<Long, String> getPrivateMemoByScheduleId(List<Long> scheduleIds, Long currentUserId) {
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }

        return schedulePrivateMemoRepository
            .findAllByScheduleIdInAndUserIdAndDeletedAtIsNull(scheduleIds, currentUserId)
            .stream()
            .collect(Collectors.toMap(
                privateMemo -> privateMemo.getSchedule().getId(),
                SchedulePrivateMemo::getContent,
                (first, second) -> first
            ));
    }

    private void validateProjectFilter(
        Long currentUserId,
        ScheduleQueryScope queryScope,
        Long projectId
    ) {
        if (projectId == null) {
            return;
        }

        if (queryScope == ScheduleQueryScope.PERSONAL) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);
    }

    @Transactional
    public ScheduleResponse createSchedule(Long currentUserId, ScheduleCreateRequest request) {
        validatePeriod(request.getStartAt(), request.getEndAt());

        Users writer = getActiveUser(currentUserId);
        Project project = resolveProject(currentUserId, request);
        Schedule schedule = Schedule.create(
            project,
            writer,
            request.getScheduleScope(),
            request.getTitle(),
            request.getStartAt(),
            request.getEndAt(),
            request.getLocation(),
            request.getPublicMemo()
        );

        Schedule savedSchedule = scheduleRepository.save(schedule);
        saveParticipants(savedSchedule, project, request.getParticipantIds());

        // TODO: 프로젝트 일정 생성 알림과 activity_log 연동은 알림 도메인 구현 후 연결한다.
        return scheduleConverter.toResponse(savedSchedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(
        Long currentUserId,
        Long scheduleId,
        ScheduleUpdateRequest request
    ) {
        validateUpdateRequest(request);

        Schedule schedule = getScheduleOrThrow(scheduleId);
        validateWriter(schedule, currentUserId);

        LocalDateTime nextStartAt = request.getStartAt() != null
            ? request.getStartAt()
            : schedule.getStartAt();
        LocalDateTime nextEndAt = request.getEndAt() != null
            ? request.getEndAt()
            : schedule.getEndAt();
        validatePeriod(nextStartAt, nextEndAt);

        schedule.updateInfo(
            request.getTitle(),
            request.getStartAt(),
            request.getEndAt(),
            request.getLocation(),
            request.getPublicMemo()
        );

        if (request.getParticipantIds() != null) {
            updateParticipants(schedule, request.getParticipantIds());
        }

        // TODO: 프로젝트 일정 수정 알림과 activity_log 연동은 알림 도메인 구현 후 연결한다.
        return scheduleConverter.toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long currentUserId, Long scheduleId) {
        Schedule schedule = getScheduleOrThrow(scheduleId);
        validateWriter(schedule, currentUserId);

        schedule.delete();
        scheduleParticipantRepository.findActiveParticipantsByScheduleId(scheduleId)
            .forEach(ScheduleParticipant::delete);
        schedulePrivateMemoRepository.findAllByScheduleIdAndDeletedAtIsNull(scheduleId)
            .forEach(SchedulePrivateMemo::delete);

        // TODO: 프로젝트 일정 삭제 알림과 activity_log 연동은 알림 도메인 구현 후 연결한다.
    }

    public ScheduleParticipantCandidateResponse getParticipantCandidates(
        Long currentUserId,
        Long projectId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        List<ProjectMember> projectMembers = projectMemberRepository.findAllActiveMembersByProjectId(projectId);
        Map<Long, List<RoleName>> roleNamesByMemberId = getRoleNamesByMemberId(projectMembers);

        List<ScheduleParticipantCandidateResponse.Candidate> candidates = projectMembers.stream()
            .map(projectMember -> {
                Users user = projectMember.getUser();

                return ScheduleParticipantCandidateResponse.Candidate.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .permission(projectMember.getPermission())
                    .jobRole(roleNamesByMemberId.getOrDefault(projectMember.getId(), List.of()))
                    .build();
            })
            .toList();

        return ScheduleParticipantCandidateResponse.builder()
            .candidates(candidates)
            .build();
    }

    private Project resolveProject(Long currentUserId, ScheduleCreateRequest request) {
        if (request.getScheduleScope() == ScheduleScope.PERSONAL) {
            if (request.getProjectId() != null || hasParticipants(request.getParticipantIds())) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }

            return null;
        }

        if (request.getScheduleScope() != ScheduleScope.PROJECT || request.getProjectId() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        Project project = projectAccessValidator.getProjectOrThrow(request.getProjectId());
        projectAccessValidator.validateProjectAccess(request.getProjectId(), currentUserId);
        if (!hasParticipants(request.getParticipantIds())) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        return project;
    }

    private void saveParticipants(Schedule schedule, Project project, List<Long> participantIds) {
        if (!schedule.isProjectSchedule()) {
            return;
        }

        List<Users> participants = getProjectParticipantUsers(project.getId(), participantIds);
        List<ScheduleParticipant> scheduleParticipants = participants.stream()
            .map(user -> ScheduleParticipant.create(schedule, user))
            .toList();

        scheduleParticipantRepository.saveAll(scheduleParticipants);
    }

    private void updateParticipants(Schedule schedule, List<Long> participantIds) {
        if (!schedule.isProjectSchedule()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        Project project = schedule.getProject();
        List<Users> requestedUsers = getProjectParticipantUsers(project.getId(), participantIds);
        Set<Long> requestedUserIds = requestedUsers.stream()
            .map(Users::getId)
            .collect(Collectors.toSet());

        List<ScheduleParticipant> currentParticipants =
            scheduleParticipantRepository.findActiveParticipantsByScheduleId(schedule.getId());
        Set<Long> currentUserIds = currentParticipants.stream()
            .map(ScheduleParticipant::getUser)
            .map(Users::getId)
            .collect(Collectors.toSet());

        currentParticipants.stream()
            .filter(participant -> !requestedUserIds.contains(participant.getUser().getId()))
            .forEach(ScheduleParticipant::delete);

        List<ScheduleParticipant> newParticipants = requestedUsers.stream()
            .filter(user -> !currentUserIds.contains(user.getId()))
            .map(user -> ScheduleParticipant.create(schedule, user))
            .toList();

        scheduleParticipantRepository.saveAll(newParticipants);
    }

    private List<Users> getProjectParticipantUsers(Long projectId, List<Long> participantIds) {
        if (!hasParticipants(participantIds)) {
            return List.of();
        }

        Set<Long> uniqueParticipantIds = new HashSet<>(participantIds);
        List<ProjectMember> projectMembers = projectMemberRepository.findAllActiveMembersByProjectId(projectId);
        Map<Long, Users> usersById = projectMembers.stream()
            .map(ProjectMember::getUser)
            .collect(Collectors.toMap(Users::getId, Function.identity()));

        if (!usersById.keySet().containsAll(uniqueParticipantIds)) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        return uniqueParticipantIds.stream()
            .map(usersById::get)
            .toList();
    }

    private Users getActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private Schedule getScheduleOrThrow(Long scheduleId) {
        return scheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private void validateWriter(Schedule schedule, Long currentUserId) {
        if (!schedule.isWriter(currentUserId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateUpdateRequest(ScheduleUpdateRequest request) {
        if (request.getTitle() == null
            && request.getStartAt() == null
            && request.getEndAt() == null
            && request.getLocation() == null
            && request.getPublicMemo() == null
            && request.getParticipantIds() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        if (request.getTitle() != null && request.getTitle().isBlank()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private boolean hasParticipants(List<Long> participantIds) {
        return participantIds != null && !participantIds.isEmpty();
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
}
