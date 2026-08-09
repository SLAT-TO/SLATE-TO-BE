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

    // 포트폴리오 1건이 작업 1건이므로 type 별 건수가 곧 "많이 한 프로젝트 유형"이다.
    // 동수일 때 순서가 흔들리지 않도록 count 다음에 type 이름으로 한 번 더 정렬한다.
    @Query("""
        select p.type, count(p.id)
        from UserPortfolio p
        where p.user.id = :userId
            and p.deletedAt is null
        group by p.type
        order by count(p.id) desc, p.type asc
        """)
    List<Object[]> findProjectTypeStatRowsByUserId(@Param("userId") Long userId);

    // 포트폴리오가 soft delete 돼도 user_portfolio_role 행은 남는다.
    // 조인해서 부모의 deleted_at 을 직접 걸러야 삭제한 작업의 역할이 통계에 남지 않는다.
    @Query("""
        select r.roleName, count(r.id)
        from UserPortfolioRole r
            join r.portfolio p
        where r.user.id = :userId
            and p.deletedAt is null
        group by r.roleName
        order by count(r.id) desc, r.roleName asc
        """)
    List<Object[]> findRoleStatRowsByUserId(@Param("userId") Long userId);

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
