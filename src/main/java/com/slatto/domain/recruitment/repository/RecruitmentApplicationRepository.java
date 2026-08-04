package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    // 유니크 제약이 없어 같은 유저의 중복 지원 행이 생길 수 있으므로 distinct 로 센다.
    @Query("""
        select count(distinct ra.user.id)
        from RecruitmentApplication ra
        where ra.recruitment.id = :recruitmentId
            and ra.deletedAt is null
        """)
    long countDistinctApplicants(@Param("recruitmentId") Long recruitmentId);

    // 목록에서 공고별 지원자 수를 한 번에 가져온다. 지원자 0명인 공고는 결과에 없으므로
    // 호출부에서 getOrDefault(id, 0L) 로 채워야 한다.
    @Query("""
        select ra.recruitment.id, count(distinct ra.user.id)
        from RecruitmentApplication ra
        where ra.recruitment.id in :recruitmentIds
            and ra.deletedAt is null
        group by ra.recruitment.id
        """)
    List<Object[]> countDistinctApplicantsByRecruitmentIds(
        @Param("recruitmentIds") Collection<Long> recruitmentIds
    );

    boolean existsByRecruitmentIdAndUserIdAndDeletedAtIsNull(Long recruitmentId, Long userId);

    // 공고 상세의 hasApplied 와 myApplicationStatus 를 한 번에 채운다.
    Optional<RecruitmentApplication> findFirstByRecruitmentIdAndUserIdAndDeletedAtIsNullOrderByIdDesc(
        Long recruitmentId,
        Long userId
    );

    // recruitmentId 조건을 반드시 함께 건다. 빼면 다른 공고의 applicationId 로 작성자 검증을 우회할 수 있다.
    Optional<RecruitmentApplication> findByIdAndRecruitmentIdAndDeletedAtIsNull(Long id, Long recruitmentId);

    @Query("""
        select ra
        from RecruitmentApplication ra
        join fetch ra.user
        where ra.recruitment.id = :recruitmentId
            and ra.deletedAt is null
            and (:status is null or ra.status = :status)
            and (:cursorId is null or ra.id < :cursorId)
        order by ra.id desc
        """)
    List<RecruitmentApplication> findApplicantsByCursor(
        @Param("recruitmentId") Long recruitmentId,
        @Param("status") RecruitmentApplicationStatus status,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    @Query("""
        select ra
        from RecruitmentApplication ra
        join fetch ra.recruitment r
        join fetch r.writer
        where ra.user.id = :currentUserId
            and ra.deletedAt is null
            and r.deletedAt is null
            and (:status is null or ra.status = :status)
            and (:cursorId is null or ra.id < :cursorId)
        order by ra.id desc
        """)
    List<RecruitmentApplication> findMyApplicationsByCursor(
        @Param("currentUserId") Long currentUserId,
        @Param("status") RecruitmentApplicationStatus status,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );
}
