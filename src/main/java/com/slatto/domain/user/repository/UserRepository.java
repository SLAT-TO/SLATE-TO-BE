package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

	Optional<Users> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

	Optional<Users> findByEmail(String email);

}
