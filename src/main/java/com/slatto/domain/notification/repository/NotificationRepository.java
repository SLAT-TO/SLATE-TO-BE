package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 그룹핑 알림은 기존 알림의 updated_at이 갱신되므로 최신순과 보존 기간 모두 updated_at 기준으로 조회한다.
    @Query("""
        select n
        from Notification n
        left join fetch n.project p
        where n.user.id = :userId
            and n.deletedAt is null
            and n.updatedAt >= :updatedAfter
            and (
                :cursorId is null
                or case when n.isRead = false then 0 else 1 end > :cursorReadOrder
                or (
                    case when n.isRead = false then 0 else 1 end = :cursorReadOrder
                    and (
                        n.updatedAt < :cursorUpdatedAt
                        or (n.updatedAt = :cursorUpdatedAt and n.id < :cursorId)
                    )
                )
            )
        order by
            case when n.isRead = false then 0 else 1 end,
            n.updatedAt desc,
            n.id desc
        """)
    List<Notification> findRecentNotificationsByCursor(
        @Param("userId") Long userId,
        @Param("updatedAfter") LocalDateTime updatedAfter,
        @Param("cursorId") Long cursorId,
        @Param("cursorReadOrder") Integer cursorReadOrder,
        @Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
        Pageable pageable
    );

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNullAndUpdatedAtGreaterThanEqual(
        Long id,
        Long userId,
        LocalDateTime updatedAfter
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.isRead = true,
            n.readAt = case when n.readAt is null then :readAt else n.readAt end
        where n.id = :notificationId
            and n.user.id = :userId
            and n.deletedAt is null
        """)
    int markAsReadByIdAndUserId(
        @Param("notificationId") Long notificationId,
        @Param("userId") Long userId,
        @Param("readAt") LocalDateTime readAt
    );

    // 동일 대상의 미읽음 그룹 알림이 있으면 누적 개수와 문구를 함께 갱신한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.content = :content,
            n.groupCount = n.groupCount + 1,
            n.updatedAt = :updatedAt
        where n.user.id = :userId
            and n.type = :type
            and n.targetType = :targetType
            and n.targetId = :targetId
            and n.isRead = false
            and n.deletedAt is null
        """)
    int incrementUnreadGroupedNotification(
        @Param("userId") Long userId,
        @Param("type") NotificationType type,
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("content") String content,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    // 미읽음 그룹 알림이 없으면 영상 카드에 표시할 개수는 0이다.
    @Query("""
        select coalesce(max(n.groupCount), 0)
        from Notification n
        where n.user.id = :userId
            and n.type = :type
            and n.targetType = :targetType
            and n.targetId = :targetId
            and n.isRead = false
            and n.deletedAt is null
        """)
    int findUnreadGroupedNotificationCount(
        @Param("userId") Long userId,
        @Param("type") NotificationType type,
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.isRead = true,
            n.readAt = :readAt
        where n.user.id = :userId
            and n.isRead = false
            and n.deletedAt is null
        """)
    int markAllAsReadByUserId(
        @Param("userId") Long userId,
        @Param("readAt") LocalDateTime readAt
    );
}
