package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectNoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectNoticeReadRepository extends JpaRepository<ProjectNoticeRead, Long> {

    Optional<ProjectNoticeRead> findByNoticeIdAndUserId(Long noticeId, Long userId);

    // 시각은 DB 의 NOW() 가 아니라 애플리케이션에서 받는다.
    // NOW() 는 MySQL 세션 타임존을 따르므로 JPA Auditing 이 쓰는 다른 시각들과 어긋난다.
    @Modifying
    @Query(value = """
        INSERT INTO project_notice_read
            (notice_id, user_id, read_at, created_at, updated_at)
        VALUES (:noticeId, :userId, :now, :now, :now)
        ON DUPLICATE KEY UPDATE read_at = :now, updated_at = :now
        """, nativeQuery = true)
    void upsertRead(
        @Param("noticeId") Long noticeId,
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
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
