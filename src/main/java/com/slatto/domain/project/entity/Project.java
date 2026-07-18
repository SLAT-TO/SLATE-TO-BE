package com.slatto.domain.project.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.global.exception.BaseException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    private static final ProjectStatus DEFAULT_STATUS = ProjectStatus.PREPARING;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private Users ownerUser;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = true)
    private CategoryName type;

    @Column(name = "custom_type_name", nullable = true, length = 100)
    private String customTypeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "length_type", nullable = true)
    private LengthType lengthType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "client_name", nullable = true, length = 255)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = true)
    private Kind kind;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private Project(
        Users ownerUser,
        String title,
        CategoryName type,
        String customTypeName,
        LengthType lengthType,
        String description,
        LocalDate endDate,
        String clientName,
        Kind kind
    ) {
        LocalDate startDate = LocalDate.now();
        validateProjectPeriod(startDate, endDate);

        this.ownerUser = ownerUser;
        this.title = title;
        this.type = type;
        this.customTypeName = customTypeName;
        this.lengthType = lengthType;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.clientName = clientName;
        this.status = DEFAULT_STATUS;
        this.kind = kind;
    }

    public static Project create(
        Users ownerUser,
        String title,
        CategoryName type,
        String customTypeName,
        LengthType lengthType,
        String description,
        LocalDate endDate,
        String clientName,
        Kind kind
    ) {
        return new Project(
            ownerUser,
            title,
            type,
            customTypeName,
            lengthType,
            description,
            endDate,
            clientName,
            kind
        );
    }

    public void updateInfo(
        String title,
        CategoryName type,
        String customTypeName,
        LengthType lengthType,
        String description,
        LocalDate endDate,
        String clientName,
        Kind kind
    ) {
        validateProjectPeriod(this.startDate, endDate);

        this.title = title;
        this.type = type;
        this.customTypeName = customTypeName;
        this.lengthType = lengthType;
        this.description = description;
        this.endDate = endDate;
        this.clientName = clientName;
        this.kind = kind;
    }

    public void changeStatus(ProjectStatus status) {
        this.status = status;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    private void validateProjectPeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate == null || endDate.isBefore(startDate)) {
            throw new BaseException(ProjectErrorCode.INVALID_PROJECT_PERIOD);
        }
    }
}
