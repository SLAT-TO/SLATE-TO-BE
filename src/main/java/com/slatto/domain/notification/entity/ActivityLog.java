package com.slatto.domain.notification.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.notification.enums.ActorType;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.model.ActivityActor;
import com.slatto.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActivityLogType type;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_type", nullable = true, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    @Column(name = "group_key", nullable = true, length = 255)
    private String groupKey;

    private ActivityLog(
        Project project,
        ActivityActor actor,
        ActivityLogType type,
        String content,
        ActivityLogTargetType targetType,
        Long targetId,
        String groupKey
    ) {
        this.project = project;
        this.actorUserId = actor.actorUserId();
        this.actorGuestId = actor.actorGuestId();
        this.actorType = actor.actorType();
        this.type = type;
        this.content = content;
        this.targetType = targetType != null ? targetType.name() : null;
        this.targetId = targetId;
        this.groupKey = groupKey;
    }

    public static ActivityLog create(
        Project project,
        ActivityActor actor,
        ActivityLogType type,
        String content,
        ActivityLogTargetType targetType,
        Long targetId
    ) {
        return new ActivityLog(
            project,
            actor,
            type,
            content,
            targetType,
            targetId,
            null
        );
    }

}
