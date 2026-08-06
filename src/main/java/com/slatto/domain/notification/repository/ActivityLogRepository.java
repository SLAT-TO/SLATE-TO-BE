package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
        select al
        from ActivityLog al
        where al.project.id = :projectId
            and (
                :cursorCreatedAt is null
                or al.createdAt < :cursorCreatedAt
                or (al.createdAt = :cursorCreatedAt and al.id < :cursorActivityId)
            )
        order by al.createdAt desc, al.id desc
        """)
    List<ActivityLog> findRecentActivitiesByCursor(
        @Param("projectId") Long projectId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorActivityId") Long cursorActivityId,
        Pageable pageable
    );

    Optional<ActivityLog> findByIdAndProjectId(Long activityId, Long projectId);

    @Query("""
        select al.project.id as projectId, max(al.createdAt) as lastActivityAt
        from ActivityLog al
        where al.project.id in :projectIds
        group by al.project.id
        """)
    List<ProjectLatestActivityProjection> findLatestActivityAtByProjectIds(
        @Param("projectIds") List<Long> projectIds
    );
}
