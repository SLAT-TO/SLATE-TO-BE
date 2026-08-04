package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    // 유니크 제약이 없어 같은 유저의 중복 지원 행이 생길 수 있으므로 distinct 로 센다.
    @Query("""
        select count(distinct ra.user.id)
        from RecruitmentApplication ra
        where ra.recruitment.id = :recruitmentId
            and ra.deletedAt is null
        """)
    long countDistinctApplicants(@Param("recruitmentId") Long recruitmentId);

    boolean existsByRecruitmentIdAndUserIdAndDeletedAtIsNull(Long recruitmentId, Long userId);
}
