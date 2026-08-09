package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.RecruitmentApplicationFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecruitmentApplicationFileRepository extends JpaRepository<RecruitmentApplicationFile, Long> {

    // 지원 요청이 넘긴 파일 id 를 검증하며 가져온다. 소유자·대상 공고·미연결 여부를 모두 WHERE 에서 거른다.
    // 조건을 통과한 건수가 요청 개수와 다르면 서비스가 400 으로 끊는다.
    @Query("""
        select f
        from RecruitmentApplicationFile f
        where f.id in :fileIds
            and f.recruitment.id = :recruitmentId
            and f.uploader.id = :uploaderId
            and f.application is null
            and f.deletedAt is null
        """)
    List<RecruitmentApplicationFile> findLinkableFiles(
        @Param("fileIds") Collection<Long> fileIds,
        @Param("recruitmentId") Long recruitmentId,
        @Param("uploaderId") Long uploaderId
    );

    @Query("""
        select f
        from RecruitmentApplicationFile f
        where f.application.id in :applicationIds
            and f.deletedAt is null
        order by f.id asc
        """)
    List<RecruitmentApplicationFile> findByApplicationIds(@Param("applicationIds") Collection<Long> applicationIds);

    @Query("""
        select f
        from RecruitmentApplicationFile f
        where f.id = :fileId
            and f.application.id = :applicationId
            and f.deletedAt is null
        """)
    Optional<RecruitmentApplicationFile> findActiveFileByApplicationIdAndFileId(
        @Param("applicationId") Long applicationId,
        @Param("fileId") Long fileId
    );
}
