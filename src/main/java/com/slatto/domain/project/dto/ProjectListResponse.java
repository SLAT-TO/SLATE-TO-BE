package com.slatto.domain.project.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.enums.Permission;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProjectListResponse {

    private List<ProjectSummary> items;

    private Long nextCursor;

    private Boolean hasNext;

    @Getter
    @Builder
    public static class ProjectSummary {

        private Long id;

        private String title;

        private CategoryName type;

        private LengthType lengthType;

        private ProjectStatus status;

        private Kind kind;

        private List<RoleName> roleNames;

        private String previewImageUrl;

        private LocalDate startDate;

        private LocalDate endDate;

        private Integer deadlineProgressPercent;

        private LocalDateTime lastActivityAt;

        private List<String> memberPreviewImageUrls;

        private Long memberCount;

        private Boolean isPinned;

        private LocalDateTime pinnedAt;

        private Permission myPermission;

        private Boolean canEdit;

        private Boolean canDelete;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }
}
