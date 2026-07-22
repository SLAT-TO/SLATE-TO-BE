package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Query("""
        UPDATE Users u
        SET u.onboardingCompleted = true
        WHERE u.id = :id AND u.onboardingCompleted = false AND u.deletedAt IS NULL
        """)
    int markOnboardingCompleted(@Param("id") Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    Optional<Users> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    Optional<Users> findByEmail(String email);
}
