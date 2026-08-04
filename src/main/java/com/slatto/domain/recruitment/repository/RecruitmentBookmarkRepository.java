package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.RecruitmentBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentBookmarkRepository extends JpaRepository<RecruitmentBookmark, Long> {

    boolean existsByRecruitmentIdAndUserId(Long recruitmentId, Long userId);
}
