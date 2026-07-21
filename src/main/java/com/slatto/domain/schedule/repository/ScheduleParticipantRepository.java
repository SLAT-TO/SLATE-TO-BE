package com.slatto.domain.schedule.repository;

import com.slatto.domain.schedule.entity.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {

    @Query("""
        select sp
        from ScheduleParticipant sp
        join fetch sp.user u
        where sp.schedule.id = :scheduleId
            and sp.deletedAt is null
        order by sp.id asc
        """)
    List<ScheduleParticipant> findActiveParticipantsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("""
        select sp
        from ScheduleParticipant sp
        join fetch sp.user u
        where sp.schedule.id in :scheduleIds
            and sp.deletedAt is null
        order by sp.schedule.id asc, sp.id asc
        """)
    List<ScheduleParticipant> findActiveParticipantsByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
}
