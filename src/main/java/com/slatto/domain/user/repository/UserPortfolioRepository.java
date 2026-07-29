package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserPortfolio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
