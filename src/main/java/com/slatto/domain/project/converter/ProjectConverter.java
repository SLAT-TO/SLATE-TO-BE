package com.slatto.domain.project.converter;

import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectResponse;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RoleName;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ProjectConverter {

    private static final int MIN_PROGRESS_PERCENT = 0;
    private static final int MAX_PROGRESS_PERCENT = 100;

    public Project toProject(Users ownerUser, ProjectCreateRequest request) {
        return Project.create(
            ownerUser,
            request.getTitle(),
            request.getType(),
            request.getCustomTypeName(),
            request.getLengthType(),
            request.getDescription(),
            request.getEndDate(),
            request.getClientName(),
            request.getKind()
        );
    }

    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .title(project.getTitle())
            .status(project.getStatus())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    public ProjectListResponse toListResponse(
        List<ProjectListResponse.ProjectSummary> items,
        Long nextCursor,
        Boolean hasNext
    ) {
        return ProjectListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    public ProjectListResponse.ProjectSummary toSummary(
        Project project,
        Long memberCount,
        List<String> memberPreviewImageUrls,
        LocalDateTime lastActivityAt
    ) {
        return ProjectListResponse.ProjectSummary.builder()
            .id(project.getId())
            .title(project.getTitle())
            .type(project.getType())
            .customTypeName(project.getCustomTypeName())
            .lengthType(project.getLengthType())
            .status(project.getStatus())
            .kind(project.getKind())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .deadlineProgressPercent(calculateDeadlineProgressPercent(project.getStartDate(), project.getEndDate()))
            .lastActivityAt(lastActivityAt)
            .memberPreviewImageUrls(memberPreviewImageUrls)
            .memberCount(memberCount)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    public ProjectDetailResponse toDetailResponse(
        Project project,
        ProjectMember currentMember,
        List<RoleName> myRoles,
        Long memberCount
    ) {
        boolean admin = currentMember.isAdmin();

        return ProjectDetailResponse.builder()
            .id(project.getId())
            .title(project.getTitle())
            .type(project.getType())
            .customTypeName(project.getCustomTypeName())
            .lengthType(project.getLengthType())
            .description(project.getDescription())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .clientName(project.getClientName())
            .status(project.getStatus())
            .kind(project.getKind())
            .owner(toOwnerSummary(project.getOwnerUser()))
            .myPermission(currentMember.getPermission())
            .myRoles(myRoles)
            .memberCount(memberCount)
            .canEdit(admin)
            .canDelete(admin)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    private ProjectDetailResponse.OwnerSummary toOwnerSummary(Users ownerUser) {
        return ProjectDetailResponse.OwnerSummary.builder()
            .id(ownerUser.getId())
            .nickname(ownerUser.getNickname())
            .profileImageUrl(ownerUser.getProfileImageUrl())
            .build();
    }

    private int calculateDeadlineProgressPercent(LocalDate startDate, LocalDate endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays <= 0) {
            return MAX_PROGRESS_PERCENT;
        }

        long elapsedDays = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        long boundedElapsedDays = Math.max(0, Math.min(elapsedDays, totalDays));
        long progressPercent = boundedElapsedDays * MAX_PROGRESS_PERCENT / totalDays;

        return Math.max(MIN_PROGRESS_PERCENT, Math.min((int) progressPercent, MAX_PROGRESS_PERCENT));
    }
}
