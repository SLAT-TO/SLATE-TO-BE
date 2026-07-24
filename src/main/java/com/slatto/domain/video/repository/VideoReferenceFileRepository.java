package com.slatto.domain.video.repository;

import com.slatto.domain.video.entity.VideoReferenceFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoReferenceFileRepository extends JpaRepository<VideoReferenceFile, Long> {

    @Query("""
        select vrf
        from VideoReferenceFile vrf
        join fetch vrf.projectFile pf
        join fetch pf.uploader uploader
        where vrf.video.project.id = :projectId
            and vrf.video.id = :videoId
            and vrf.deletedAt is null
            and pf.deletedAt is null
            and (:keyword is null or lower(pf.fileName) like lower(concat('%', :keyword, '%')))
            and (:cursor is null or vrf.id < :cursor)
        order by vrf.id desc
        """)
    List<VideoReferenceFile> findActiveReferenceFilesByCursor(
        @Param("projectId") Long projectId,
        @Param("videoId") Long videoId,
        @Param("keyword") String keyword,
        @Param("cursor") Long cursor,
        Pageable pageable
    );

    @Query("""
        select count(vrf)
        from VideoReferenceFile vrf
        where vrf.video.project.id = :projectId
            and vrf.video.id = :videoId
            and vrf.projectFile.id = :projectFileId
            and vrf.deletedAt is null
        """)
    long countActiveReferenceFile(
        @Param("projectId") Long projectId,
        @Param("videoId") Long videoId,
        @Param("projectFileId") Long projectFileId
    );

    @Query("""
        select vrf
        from VideoReferenceFile vrf
        join fetch vrf.projectFile pf
        join fetch pf.uploader uploader
        where vrf.video.project.id = :projectId
            and vrf.video.id = :videoId
            and vrf.id = :referenceFileId
            and vrf.deletedAt is null
            and pf.deletedAt is null
        """)
    Optional<VideoReferenceFile> findActiveReferenceFile(
        @Param("projectId") Long projectId,
        @Param("videoId") Long videoId,
        @Param("referenceFileId") Long referenceFileId
    );
}
