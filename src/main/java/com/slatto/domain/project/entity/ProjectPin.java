package com.slatto.domain.project.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "project_pin",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_project_pin_user_project",
        columnNames = {"user_id", "project_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectPin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;

    private ProjectPin(Users user, Project project) {
        this.user = user;
        this.project = project;
        this.pinnedAt = LocalDateTime.now();
    }

    public static ProjectPin create(Users user, Project project) {
        return new ProjectPin(user, project);
    }
}
