package com.slatto.domain.feedback.repository;

import com.slatto.domain.feedback.entity.FeedbackDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackDetailRepository extends JpaRepository<FeedbackDetail, Long> {

    // 첫 페이지
    @Query("""
            select fd from FeedbackDetail fd
            where fd.feedback.id = :feedbackId
              and fd.deletedAt is null
            order by fd.id asc
            """)
    List<FeedbackDetail> findFirstPage(@Param("feedbackId") Long feedbackId, Pageable pageable);

    // 다음 페이지
    @Query("""
            select fd from FeedbackDetail fd
            where fd.feedback.id = :feedbackId
              and fd.deletedAt is null
              and fd.id > :cursor
            order by fd.id asc
            """)
    List<FeedbackDetail> findNextPage(@Param("feedbackId") Long feedbackId,
                                      @Param("cursor") Long cursor,
                                      Pageable pageable);
}