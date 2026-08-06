package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.Notification;
import com.slatto.domain.notification.enums.NotificationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 브리핑 우선순위를 먼저 적용한 뒤 필요한 후보만 조회한다.
    @Query("""
        select n
        from Notification n
        left join fetch n.project p
        where n.user.id = :userId
            and n.deletedAt is null
            and n.updatedAt >= :updatedAfter
            and n.type in :types
        order by
            case
                when n.type = :recruitmentAppliedType then 3
                when n.type = :scheduleCreatedType then 4
                when n.type = :noticeCreatedType then 5
                when n.type = :fileUploadedType then 6
                when n.type = :videoFeedbackCommentedType then 7
                else 999
            end,
            n.updatedAt desc,
            n.id desc
        """)
    List<Notification> findRecentBriefingCandidates(
        @Param("userId") Long userId,
        @Param("updatedAfter") LocalDateTime updatedAfter,
        @Param("types") List<NotificationType> types,
        @Param("recruitmentAppliedType") NotificationType recruitmentAppliedType,
        @Param("scheduleCreatedType") NotificationType scheduleCreatedType,
        @Param("noticeCreatedType") NotificationType noticeCreatedType,
        @Param("fileUploadedType") NotificationType fileUploadedType,
        @Param("videoFeedbackCommentedType") NotificationType videoFeedbackCommentedType,
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

    // 동일 대상의 미읽음 그룹 알림을 잠금 조회해 누적 개수와 문구를 같은 엔티티 상태 기준으로 갱신한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select n
        from Notification n
        where n.user.id = :userId
            and n.type = :type
            and n.targetType = :targetType
            and n.targetId = :targetId
            and n.isRead = false
            and n.deletedAt is null
        order by n.updatedAt desc, n.id desc
        """)
    List<Notification> findUnreadGroupedNotificationsForUpdate(
        @Param("userId") Long userId,
        @Param("type") NotificationType type,
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        Pageable pageable
    );

    @Query("""
        select n.targetId, coalesce(max(n.groupCount), 0)
        from Notification n
        where n.user.id = :userId
            and n.type = :type
            and n.targetType = :targetType
            and n.targetId in :targetIds
            and n.isRead = false
            and n.deletedAt is null
        group by n.targetId
        """)
    List<Object[]> findUnreadGroupedNotificationCounts(
        @Param("userId") Long userId,
        @Param("type") NotificationType type,
        @Param("targetType") String targetType,
        @Param("targetIds") List<Long> targetIds
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
