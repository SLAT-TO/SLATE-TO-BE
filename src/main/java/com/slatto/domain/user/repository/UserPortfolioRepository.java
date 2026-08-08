package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserPortfolio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    Optional<UserPortfolio> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Query("""
        select p
        from UserPortfolio p
        where p.user.id = :userId
            and p.deletedAt is null
            and (:cursor is null or p.id < :cursor)
        order by p.id desc
        """)
    List<UserPortfolio> findActivePortfoliosByCursor(
        @Param("userId") Long userId,
        @Param("cursor") Long cursor,
        Pageable pageable
    );

    // 탈퇴 시 포트폴리오를 한 번에 내린다. 건별 로딩 없이 처리하려고 벌크 업데이트를 쓴다.
    // clearAutomatically 는 쓰지 않는다. 같은 트랜잭션에 로딩된 Users 가 detach 된다.
    @Modifying
    @Query("""
        update UserPortfolio p
        set p.deletedAt = :deletedAt
        where p.user.id = :userId
            and p.deletedAt is null
        """)
    int softDeleteAllByUserId(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
