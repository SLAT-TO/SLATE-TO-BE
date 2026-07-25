package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectPinRepository extends JpaRepository<ProjectPin, Long> {

    Optional<ProjectPin> findByUserIdAndProjectId(Long userId, Long projectId);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO project_pin
            (user_id, project_id, pinned_at, created_at, updated_at)
        VALUES (:userId, :projectId, NOW(6), NOW(6), NOW(6))
        """, nativeQuery = true)
    void insertIgnore(
        @Param("userId") Long userId,
        @Param("projectId") Long projectId
    );

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
