package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    @Query("""
        select pf
        from ProjectFile pf
        join fetch pf.uploader u
        where pf.project.id = :projectId
            and pf.deletedAt is null
            and (:keyword is null
                or :keyword = ''
                or lower(pf.fileName) like lower(concat('%', :keyword, '%')))
            and (:cursor is null or pf.id < :cursor)
        order by
            case when pf.pinnedAt is null then 1 else 0 end asc,
            pf.pinnedAt desc,
            pf.id desc
        """)
    List<ProjectFile> findActiveFilesByCursor(
        @Param("projectId") Long projectId,
        @Param("keyword") String keyword,
        @Param("cursor") Long cursor,
        Pageable pageable
    );

    @Query("""
        select pf
        from ProjectFile pf
        join fetch pf.project p
        join fetch pf.uploader u
        where p.id = :projectId
            and pf.id = :fileId
            and pf.deletedAt is null
        """)
    Optional<ProjectFile> findActiveFileByProjectIdAndFileId(
        @Param("projectId") Long projectId,
        @Param("fileId") Long fileId
    );
}
