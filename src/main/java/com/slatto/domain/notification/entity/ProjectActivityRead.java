package com.slatto.domain.notification.entity;

import com.slatto.domain.project.entity.ProjectMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "project_activity_read",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_project_activity_read_member_log",
        columnNames = {"project_member_id", "activity_log_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectActivityRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember projectMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id", nullable = false)
    private ActivityLog activityLog;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    private ProjectActivityRead(ProjectMember projectMember, ActivityLog activityLog) {
        this.projectMember = projectMember;
        this.activityLog = activityLog;
        this.readAt = LocalDateTime.now();
    }

    public static ProjectActivityRead create(ProjectMember projectMember, ActivityLog activityLog) {
        return new ProjectActivityRead(projectMember, activityLog);
    }
}
