package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    Optional<UserPortfolio> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
