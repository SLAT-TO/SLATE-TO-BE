package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.ProjectActivityRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface ProjectActivityReadRepository extends JpaRepository<ProjectActivityRead, Long> {

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
