package com.slatto.domain.auth.entity;

import com.slatto.domain.auth.enums.VerificationPurpose;
import com.slatto.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "email_verification",
	indexes = @Index(name = "idx_email_verification_email_purpose", columnList = "email, purpose, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose", nullable = false, length = 30)
	private VerificationPurpose purpose;

	// 평문 인증번호는 저장하지 않는다. SHA-256 hex 64자다.
	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "verified_at", nullable = true)
	private LocalDateTime verifiedAt;

	@Column(name = "consumed_at", nullable = true)
	private LocalDateTime consumedAt;

	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount;

	private EmailVerification(
		String email,
		VerificationPurpose purpose,
		String codeHash,
		LocalDateTime expiresAt
	) {
		this.email = email;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.attemptCount = 0;
	}

	public static EmailVerification issue(
		String email,
		VerificationPurpose purpose,
		String codeHash,
		LocalDateTime expiresAt
	) {
		return new EmailVerification(email, purpose, codeHash, expiresAt);
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

	public boolean isVerified() {
		return verifiedAt != null;
	}

	public boolean matches(String candidateCodeHash) {
		return codeHash.equals(candidateCodeHash);
	}

	public boolean hasAttemptsLeft(int maxAttempts) {
		return attemptCount < maxAttempts;
	}

	// 인증 확인 후 회원가입·재설정을 마쳐야 하는 제한 시간이다.
	public boolean isVerificationAlive(LocalDateTime now, Duration verifiedValidity) {
		return isVerified()
			&& !isConsumed()
			&& verifiedAt.plus(verifiedValidity).isAfter(now);
	}

	public LocalDateTime verifiedUntil(Duration verifiedValidity) {
		return verifiedAt == null ? null : verifiedAt.plus(verifiedValidity);
	}

	public void increaseAttemptCount() {
		this.attemptCount++;
	}

	// 시도 횟수를 소진했거나 새 코드가 발급되면 이 행을 즉시 만료시킨다.
	public void invalidate(LocalDateTime now) {
		this.expiresAt = now.minusNanos(1);
	}

	public void markVerified(LocalDateTime now) {
		this.verifiedAt = now;
	}

	public void markConsumed(LocalDateTime now) {
		this.consumedAt = now;
	}

}
