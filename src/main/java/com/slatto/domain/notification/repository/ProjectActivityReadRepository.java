package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.ProjectActivityRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface ProjectActivityReadRepository extends JpaRepository<ProjectActivityRead, Long> {

    boolean existsByProjectMemberIdAndActivityLogId(Long projectMemberId, Long activityLogId);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT :projectMemberId, activityLog.id, CURRENT_TIMESTAMP
        FROM activity_log activityLog
        WHERE activityLog.project_id = :projectId
        """, nativeQuery = true)
    int insertAllIfAbsentByProjectId(
        @Param("projectMemberId") Long projectMemberId,
        @Param("projectId") Long projectId
    );

    @Query("""
        select activityRead.activityLog.id
        from ProjectActivityRead activityRead
        where activityRead.projectMember.id = :projectMemberId
            and activityRead.activityLog.id in :activityLogIds
        """)
    Set<Long> findReadActivityLogIds(
        @Param("projectMemberId") Long projectMemberId,
        @Param("activityLogIds") Collection<Long> activityLogIds
    );
}
