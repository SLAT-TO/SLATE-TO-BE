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
    name = "project_notice_read",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_project_notice_read_notice_user",
        columnNames = {"notice_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectNoticeRead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private ProjectNotice notice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    private ProjectNoticeRead(ProjectNotice notice, Users user) {
        this.notice = notice;
        this.user = user;
        this.readAt = LocalDateTime.now();
    }

    public static ProjectNoticeRead create(ProjectNotice notice, Users user) {
        return new ProjectNoticeRead(notice, user);
    }

    public void refreshReadAt() {
        this.readAt = LocalDateTime.now();
    }
}
