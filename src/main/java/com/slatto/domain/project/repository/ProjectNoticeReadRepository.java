package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectNoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectNoticeReadRepository extends JpaRepository<ProjectNoticeRead, Long> {

    Optional<ProjectNoticeRead> findByNoticeIdAndUserId(Long noticeId, Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO project_notice_read
            (notice_id, user_id, read_at, created_at, updated_at)
        VALUES (:noticeId, :userId, NOW(6), NOW(6), NOW(6))
        ON DUPLICATE KEY UPDATE read_at = NOW(6), updated_at = NOW(6)
        """, nativeQuery = true)
    void upsertRead(
        @Param("noticeId") Long noticeId,
        @Param("userId") Long userId
    );

    @Query("""
        select pnr
        from ProjectNoticeRead pnr
        join fetch pnr.notice pn
        where pnr.user.id = :userId
            and pn.id in :noticeIds
        """)
    List<ProjectNoticeRead> findAllByUserIdAndNoticeIds(
        @Param("userId") Long userId,
        @Param("noticeIds") Collection<Long> noticeIds
    );
}
