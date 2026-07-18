package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);
}
