package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserPortfolioRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserPortfolioRoleRepository extends JpaRepository<UserPortfolioRole, Long> {

    List<UserPortfolioRole> findAllByPortfolioIdOrderByIdAsc(Long portfolioId);

    List<UserPortfolioRole> findAllByPortfolioIdInOrderByIdAsc(Collection<Long> portfolioIds);

    void deleteByPortfolioId(Long portfolioId);
}
