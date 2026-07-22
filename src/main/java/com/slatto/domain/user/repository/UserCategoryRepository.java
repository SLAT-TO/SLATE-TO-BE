package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {

    List<UserCategory> findAllByUserIdOrderByIdAsc(Long userId);
}
