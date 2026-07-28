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

    @Query("""
        select n
        from Notification n
        left join fetch n.project p
        where n.user.id = :userId
            and n.deletedAt is null
            and n.createdAt >= :createdAfter
            and (
                :cursorId is null
                or case when n.isRead = false then 0 else 1 end > :cursorReadOrder
                or (
                    case when n.isRead = false then 0 else 1 end = :cursorReadOrder
                    and (
                        n.createdAt < :cursorCreatedAt
                        or (n.createdAt = :cursorCreatedAt and n.id < :cursorId)
                    )
                )
            )
        order by
            case when n.isRead = false then 0 else 1 end,
            n.createdAt desc,
            n.id desc
        """)
    List<Notification> findRecentNotificationsByCursor(
        @Param("userId") Long userId,
        @Param("createdAfter") LocalDateTime createdAfter,
        @Param("cursorId") Long cursorId,
        @Param("cursorReadOrder") Integer cursorReadOrder,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        Pageable pageable
    );

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
        Long id,
        Long userId,
        LocalDateTime createdAfter
    );

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Optional<Notification> findTopByUserIdAndTypeAndTargetTypeAndTargetIdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
        Long userId,
        NotificationType type,
        String targetType,
        Long targetId
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
