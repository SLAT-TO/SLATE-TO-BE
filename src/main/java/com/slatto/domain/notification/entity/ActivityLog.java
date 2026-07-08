package com.slatto.domain.notification.entity;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.notification.enums.ActorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "actor_user_id", nullable = true)
    private Long actorUserId;

    @Column(name = "actor_guest_id", nullable = true)
    private Long actorGuestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "type", nullable = false, length = 500)
    private String type;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_type", nullable = true, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    @Column(name = "group_key", nullable = true, length = 255)
    private String groupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}