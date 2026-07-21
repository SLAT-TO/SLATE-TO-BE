package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectNotice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectNoticeRepository extends JpaRepository<ProjectNotice, Long> {

    @Query("""
        select pn
        from ProjectNotice pn
        join fetch pn.writer w
        where pn.project.id = :projectId
            and pn.deletedAt is null
            and (:cursor is null or pn.id < :cursor)
        order by pn.id desc
        """)
    List<ProjectNotice> findActiveNoticesByCursor(
        @Param("projectId") Long projectId,
        @Param("cursor") Long cursor,
        Pageable pageable
    );

    @Query("""
        select pn
        from ProjectNotice pn
        join fetch pn.writer w
        where pn.project.id = :projectId
            and pn.id = :noticeId
            and pn.deletedAt is null
        """)
    Optional<ProjectNotice> findActiveNoticeByProjectIdAndNoticeId(
        @Param("projectId") Long projectId,
        @Param("noticeId") Long noticeId
    );
}
