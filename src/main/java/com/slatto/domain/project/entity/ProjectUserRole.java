package com.slatto.domain.project.entity;

import com.slatto.domain.user.enums.RoleName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_user_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember projectMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private RoleName roleName;
}
