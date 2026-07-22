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
public class ProjectDetailResponse {

    private Long id;

    private String title;

    private CategoryName type;

    private LengthType lengthType;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private String clientName;

    private ProjectStatus status;

    private Kind kind;

    private OwnerSummary owner;

    private Permission myPermission;

    private List<RoleName> myRoles;

    private Long memberCount;

    private Boolean canEdit;

    private Boolean canDelete;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class OwnerSummary {

        private Long id;

        private String nickname;

        private String profileImageUrl;
    }
}
