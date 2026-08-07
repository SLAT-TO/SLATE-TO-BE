package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectPinRepository extends JpaRepository<ProjectPin, Long> {

    Optional<ProjectPin> findByUserIdAndProjectId(Long userId, Long projectId);

    // 시각은 DB 의 NOW() 가 아니라 애플리케이션에서 받는다.
    // NOW() 는 MySQL 세션 타임존을 따르므로 JPA Auditing 이 쓰는 다른 시각들과 어긋난다.
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO project_pin
            (user_id, project_id, pinned_at, created_at, updated_at)
        VALUES (:userId, :projectId, :now, :now, :now)
        """, nativeQuery = true)
    void insertIgnore(
        @Param("userId") Long userId,
        @Param("projectId") Long projectId,
        @Param("now") LocalDateTime now
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
