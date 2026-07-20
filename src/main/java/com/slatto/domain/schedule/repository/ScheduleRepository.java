package com.slatto.domain.schedule.repository;

import com.slatto.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndDeletedAtIsNull(Long id);
}
