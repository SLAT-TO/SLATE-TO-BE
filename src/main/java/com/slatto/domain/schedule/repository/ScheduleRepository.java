package com.slatto.domain.schedule.repository;

import com.slatto.domain.schedule.entity.Schedule;
import com.slatto.domain.schedule.enums.ScheduleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select distinct s
        from Schedule s
        left join fetch s.project p
        where s.deletedAt is null
            and s.startAt < :endAt
            and s.endAt >= :startAt
            and (:scope is null or s.scheduleScope = :scope)
            and (:projectId is null or p.id = :projectId)
            and (
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PERSONAL
                    and s.writer.id = :userId)
                or
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PROJECT
                    and exists (
                        select 1
                        from ProjectMember pm
                        where pm.project = p
                            and pm.user.id = :userId
                            and pm.leftAt is null
                    ))
            )
        order by s.startAt asc, s.id asc
        """)
    List<Schedule> findVisibleSchedulesBetween(
        @Param("userId") Long userId,
        @Param("scope") ScheduleScope scope,
        @Param("projectId") Long projectId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        select distinct s
        from Schedule s
        left join fetch s.project p
        where s.deletedAt is null
            and s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PROJECT
            and p.id = :projectId
            and s.startAt < :endAt
            and s.endAt >= :startAt
            and (:mineOnly = false or exists (
                select 1
                from ScheduleParticipant sp
                where sp.schedule = s
                    and sp.user.id = :userId
                    and sp.deletedAt is null
            ))
        order by s.startAt asc, s.id asc
        """)
    List<Schedule> findProjectSchedulesBetween(
        @Param("userId") Long userId,
        @Param("projectId") Long projectId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("mineOnly") boolean mineOnly
    );
}
