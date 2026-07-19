package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    Optional<Users> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    Optional<Users> findByEmail(String email);
}
