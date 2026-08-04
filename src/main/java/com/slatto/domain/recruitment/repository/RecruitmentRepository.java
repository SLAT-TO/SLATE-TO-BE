package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Optional<Recruitment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.id = :recruitmentId
            and r.deletedAt is null
        """)
    Optional<Recruitment> findActiveWithWriterById(@Param("recruitmentId") Long recruitmentId);

    // 조회수 증가는 updated_at 을 갱신하면 안 되므로 더티 체킹 대신 벌크 업데이트를 쓴다.
    // 본인 공고 제외 조건을 WHERE 절에 넣어, 엔티티 로딩 없이 증가 여부를 판정한다.
    // 반드시 엔티티 로딩보다 먼저 호출할 것 (RecruitmentService.getRecruitment 참조).
    @Modifying
    @Query("""
        update Recruitment r
        set r.viewCount = r.viewCount + 1
        where r.id = :recruitmentId
            and r.deletedAt is null
            and r.writer.id <> :currentUserId
        """)
    int increaseViewCount(
        @Param("recruitmentId") Long recruitmentId,
        @Param("currentUserId") Long currentUserId
    );
}
