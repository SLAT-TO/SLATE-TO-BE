package com.slatto.domain.auth.repository;

import com.slatto.domain.auth.entity.EmailVerification;
import com.slatto.domain.auth.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	Optional<EmailVerification> findFirstByEmailAndPurposeOrderByIdDesc(
		String email,
		VerificationPurpose purpose
	);

	// 시간당 발송 한도 계산용. 만료 여부와 무관하게 발송 이력 자체를 센다.
	long countByEmailAndPurposeAndCreatedAtAfter(
		String email,
		VerificationPurpose purpose,
		LocalDateTime createdAtAfter
	);

	// 별도 정리 스케줄러를 두지 않는다. 새 인증번호를 발송할 때 같은 이메일의 죽은 행을 함께 지운다.
	// threshold 는 시간당 한도 집계 구간보다 앞서야 한다. 최근 1시간 행을 지우면 발송 횟수가 리셋된다.
	// 그 시점이면 인증 유효 시간(30분)도 이미 지났으므로 인증 여부로 거르지 않는다.
	// 거르면 인증만 하고 가입하지 않은 행이 영구히 남는다.
	// clearAutomatically 를 쓰지 않는다. 영속 엔티티를 detach 시켜 이후 조회가 꼬인다.
	@Modifying
	@Query("""
		delete from EmailVerification ev
		where ev.email = :email
			and ev.expiresAt < :threshold
		""")
	int deleteDeadRows(
		@Param("email") String email,
		@Param("threshold") LocalDateTime threshold
	);

}
