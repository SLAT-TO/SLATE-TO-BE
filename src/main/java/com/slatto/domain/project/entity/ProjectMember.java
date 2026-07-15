package com.slatto.domain.project.entity;

import com.slatto.domain.project.enums.Permission;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private Permission permission;

    @Column(name = "joined_at", nullable = true)
    private LocalDateTime joinedAt;

    @Column(name = "left_at", nullable = true)
    private LocalDateTime leftAt;

    private ProjectMember(Project project, Users user, Permission permission) {
        this.project = project;
        this.user = user;
        this.permission = permission;
        this.joinedAt = LocalDateTime.now();
    }

    public static ProjectMember createAdmin(Project project, Users user) {
        return new ProjectMember(project, user, Permission.ADMIN);
    }

    public static ProjectMember createMember(Project project, Users user) {
        return new ProjectMember(project, user, Permission.MEMBER);
    }

    public boolean isAdmin() {
        return permission == Permission.ADMIN;
    }

    public boolean isMemberOf(Long userId) {
        return user.getId().equals(userId);
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }
}
