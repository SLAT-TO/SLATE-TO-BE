package com.slatto.domain.feedback.repository;

import com.slatto.domain.feedback.entity.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 첫 페이지 (커서 없음)
    @Query("""
            select f from Feedback f
            where f.video.id = :videoId
              and f.deletedAt is null
            order by case when f.startTime is null then 1 else 0 end asc,
                     f.startTime asc,
                     f.id asc
            """)
    List<Feedback> findFirstPage(@Param("videoId") Long videoId, Pageable pageable);

    // 다음 페이지 — 커서의 startTime이 있는 경우
    @Query("""
            select f from Feedback f
            where f.video.id = :videoId
              and f.deletedAt is null
              and (
                    f.startTime > :cursorStartTime
                 or (f.startTime = :cursorStartTime and f.id > :cursorId)
                 or f.startTime is null
              )
            order by case when f.startTime is null then 1 else 0 end asc,
                     f.startTime asc,
                     f.id asc
            """)
    List<Feedback> findNextPageWithStartTime(@Param("videoId") Long videoId,
                                             @Param("cursorStartTime") Long cursorStartTime,
                                             @Param("cursorId") Long cursorId,
                                             Pageable pageable);

    // 다음 페이지 — 커서의 startTime이 null인 경우 (이미 null 구간에 진입)
    @Query("""
            select f from Feedback f
            where f.video.id = :videoId
              and f.deletedAt is null
              and f.startTime is null
              and f.id > :cursorId
            order by f.id asc
            """)
    List<Feedback> findNextPageWithoutStartTime(@Param("videoId") Long videoId,
                                                @Param("cursorId") Long cursorId,
                                                Pageable pageable);
}