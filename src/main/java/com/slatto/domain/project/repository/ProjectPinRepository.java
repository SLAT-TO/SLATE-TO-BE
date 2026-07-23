package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectPinRepository extends JpaRepository<ProjectPin, Long> {

    Optional<ProjectPin> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    @Query("""
        select pp
        from ProjectPin pp
        join fetch pp.project p
        where pp.user.id = :userId
            and p.id in :projectIds
        """)
    List<ProjectPin> findAllByUserIdAndProjectIds(
        @Param("userId") Long userId,
        @Param("projectIds") Collection<Long> projectIds
    );
}
