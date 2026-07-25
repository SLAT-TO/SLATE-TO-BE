package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    boolean existsByProjectIdAndUserIdAndLeftAtIsNull(Long projectId, Long userId);

    long countByProjectIdAndLeftAtIsNull(Long projectId);

    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.user u
        where pm.project.id = :projectId
            and pm.leftAt is null
        order by pm.id asc
        """)
    List<ProjectMember> findAllActiveMembersByProjectId(@Param("projectId") Long projectId);

    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.user u
        where pm.project.id = :projectId
            and pm.id = :memberId
            and pm.leftAt is null
        """)
    Optional<ProjectMember> findActiveMemberByProjectIdAndMemberId(
        @Param("projectId") Long projectId,
        @Param("memberId") Long memberId
    );

    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.user u
        where pm.project.id = :projectId
            and pm.leftAt is null
        order by pm.id asc
        """)
    List<ProjectMember> findActiveMembersByProjectId(
        @Param("projectId") Long projectId,
        Pageable pageable
    );

    @Query("""
        select pm
        from ProjectMember pm
        join fetch pm.project p
        left join ProjectPin pp on pp.project = p and pp.user.id = :userId
        where pm.user.id = :userId
            and pm.leftAt is null
            and p.deletedAt is null
            and (:status is null or p.status = :status)
            and (
                :cursor is null
                or (
                    :cursorPinnedAt is not null
                    and (
                        (pp.pinnedAt is not null
                            and (pp.pinnedAt < :cursorPinnedAt
                                or (pp.pinnedAt = :cursorPinnedAt and p.id < :cursor)))
                        or pp.pinnedAt is null
                    )
                )
                or (
                    :cursorPinnedAt is null
                    and pp.pinnedAt is null
                    and p.id < :cursor
                )
            )
        order by
            case when pp.pinnedAt is null then 1 else 0 end asc,
            pp.pinnedAt desc,
            p.id desc
        """)
    List<ProjectMember> findJoinedProjectsByCursor(
        @Param("userId") Long userId,
        @Param("status") ProjectStatus status,
        @Param("cursor") Long cursor,
        @Param("cursorPinnedAt") LocalDateTime cursorPinnedAt,
        Pageable pageable
    );
}
