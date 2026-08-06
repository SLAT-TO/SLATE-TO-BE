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
	// 아직 쓸 수 있는 인증(verified 유효 구간)은 남겨야 하므로 consumed 이거나 인증 전인 행만 대상으로 한다.
	// threshold 는 시간당 한도 집계 구간보다 앞서야 한다. 최근 1시간 행을 지우면 발송 횟수가 리셋된다.
	// clearAutomatically 를 쓰지 않는다. 영속 엔티티를 detach 시켜 이후 조회가 꼬인다.
	@Modifying
	@Query("""
		delete from EmailVerification ev
		where ev.email = :email
			and ev.expiresAt < :threshold
			and (ev.verifiedAt is null or ev.consumedAt is not null)
		""")
	int deleteDeadRows(
		@Param("email") String email,
		@Param("threshold") LocalDateTime threshold
	);

}
