package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.RecruitmentBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RecruitmentBookmarkRepository extends JpaRepository<RecruitmentBookmark, Long> {

    boolean existsByRecruitmentIdAndUserId(Long recruitmentId, Long userId);

    // exists 후 save 는 동시 요청에서 유니크 위반(500)이 난다.
    // uq_recruitment_bookmark_user_recruitment 를 이용해 DB 가 멱등성을 보장하게 한다.
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO recruitment_bookmark
            (user_id, recruitment_id, created_at, updated_at)
        VALUES (:userId, :recruitmentId, :now, :now)
        """, nativeQuery = true)
    void insertIgnore(
        @Param("userId") Long userId,
        @Param("recruitmentId") Long recruitmentId,
        @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
        delete from RecruitmentBookmark b
        where b.user.id = :userId
            and b.recruitment.id = :recruitmentId
        """)
    int deleteByUserIdAndRecruitmentId(
        @Param("userId") Long userId,
        @Param("recruitmentId") Long recruitmentId
    );

    // 커서는 공고 id 가 아니라 북마크 id 다. 관심 등록 순서로 페이지를 넘긴다.
    @Query("""
        select b
        from RecruitmentBookmark b
        join fetch b.recruitment r
        join fetch r.writer
        where b.user.id = :currentUserId
            and r.deletedAt is null
            and (:cursorId is null or b.id < :cursorId)
        order by b.id desc
        """)
    List<RecruitmentBookmark> findMyBookmarksByCursor(
        @Param("currentUserId") Long currentUserId,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    @Query("""
        select b.recruitment.id
        from RecruitmentBookmark b
        where b.user.id = :userId
            and b.recruitment.id in :recruitmentIds
        """)
    List<Long> findBookmarkedRecruitmentIds(
        @Param("userId") Long userId,
        @Param("recruitmentIds") Collection<Long> recruitmentIds
    );
}
