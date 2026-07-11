package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    boolean existsByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    long countByProjectIdAndLeftAtIsNull(Long projectId);

    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.project p
        where pm.user.id = :userId
            and pm.leftAt is null
            and p.deletedAt is null
            and (:status is null or p.status = :status)
            and (:cursor is null or p.id < :cursor)
        order by p.id desc
        """)
    List<ProjectMember> findJoinedProjectsByCursor(
        @Param("userId") Long userId,
        @Param("status") ProjectStatus status,
        @Param("cursor") Long cursor,
        Pageable pageable
    );
}
