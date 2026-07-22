package com.slatto.domain.feedback.repository;

import com.slatto.domain.feedback.entity.FeedbackDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackDetailRepository extends JpaRepository<FeedbackDetail, Long> {
}