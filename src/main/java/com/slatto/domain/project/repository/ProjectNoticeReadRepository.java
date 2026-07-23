package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectNoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectNoticeReadRepository extends JpaRepository<ProjectNoticeRead, Long> {

    Optional<ProjectNoticeRead> findByNoticeIdAndUserId(Long noticeId, Long userId);

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
