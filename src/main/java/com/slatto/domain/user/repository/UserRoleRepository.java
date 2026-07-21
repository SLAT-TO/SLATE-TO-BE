package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findAllByUserIdOrderByIdAsc(Long userId);
}
