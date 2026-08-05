package com.slatto.domain.notification.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.notification.enums.NotificationType;
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

    private static final boolean DEFAULT_READ_STATUS = false;
    private static final int DEFAULT_GROUP_COUNT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = true, length = 255)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "target_type", nullable = true, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    // 그룹핑 알림에서 누적된 이벤트 개수를 저장한다. 일반 알림은 기본값 1을 사용한다.
    @Column(name = "group_count", nullable = false)
    private Integer groupCount = DEFAULT_GROUP_COUNT;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at", nullable = true)
    private LocalDateTime readAt;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    public static Notification create(
        Users user,
        Project project,
        NotificationType type,
        String title,
        String content,
        String targetType,
        Long targetId
    ) {
        Notification notification = new Notification();
        notification.user = user;
        notification.project = project;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.targetType = targetType;
        notification.targetId = targetId;
        notification.groupCount = DEFAULT_GROUP_COUNT;
        notification.isRead = DEFAULT_READ_STATUS;

        return notification;
    }

    public void markAsRead() {
        if (Boolean.TRUE.equals(isRead)) {
            return;
        }

        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void updateGroupedNotification(String title, String content) {
        this.title = title;
        this.content = content;
        this.groupCount += 1;
    }
}
