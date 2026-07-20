package com.slatto.domain.schedule.repository;

import com.slatto.domain.schedule.entity.SchedulePrivateMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulePrivateMemoRepository extends JpaRepository<SchedulePrivateMemo, Long> {

    Optional<SchedulePrivateMemo> findByScheduleIdAndUserIdAndDeletedAtIsNull(Long scheduleId, Long userId);
}
