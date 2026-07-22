package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(Long userId);
}
