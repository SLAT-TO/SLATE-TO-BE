package com.slatto.domain.notification.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

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

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_type", nullable = true, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}