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

    // 오늘의 브리핑에서 현재 사용자가 담당한 일정 후보를 기간 기준으로 조회한다.
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
            and s.startAt < :endAt
            and s.endAt >= :startAt
            and (
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PERSONAL
                    and s.writer.id = :userId)
                or
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PROJECT
                    and exists (
                        select 1
                        from ScheduleParticipant sp
                        where sp.schedule = s
                            and sp.user.id = :userId
                            and sp.deletedAt is null
                    ))
            )
        order by s.startAt asc, s.id asc
        """)
    List<Schedule> findBriefingAssignedSchedulesBetween(
        @Param("userId") Long userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    // 오늘의 브리핑에서 현재 사용자가 담당한 일정 중 해당 기간에 시작하는 일정만 조회한다.
    @Query("""
        select distinct s
        from Schedule s
        left join fetch s.project p
        where s.deletedAt is null
            and s.startAt >= :startAt
            and s.startAt < :endAt
            and (
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PERSONAL
                    and s.writer.id = :userId)
                or
                (s.scheduleScope = com.slatto.domain.schedule.enums.ScheduleScope.PROJECT
                    and exists (
                        select 1
                        from ScheduleParticipant sp
                        where sp.schedule = s
                            and sp.user.id = :userId
                            and sp.deletedAt is null
                    ))
            )
        order by s.startAt asc, s.id asc
        """)
    List<Schedule> findBriefingAssignedSchedulesStartingBetween(
        @Param("userId") Long userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

}
