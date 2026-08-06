package com.slatto.domain.auth.service;

import com.slatto.domain.auth.dto.EmailVerificationConfirmResponse;
import com.slatto.domain.auth.dto.EmailVerificationSendResponse;
import com.slatto.domain.auth.entity.EmailVerification;
import com.slatto.domain.auth.enums.VerificationPurpose;
import com.slatto.domain.auth.exception.AuthErrorCode;
import com.slatto.domain.auth.repository.EmailVerificationRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.config.properties.EmailVerificationProperties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int CODE_BOUND = 1_000_000;
	private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

	private final EmailVerificationRepository emailVerificationRepository;
	private final UserRepository userRepository;
	private final EmailVerificationProperties properties;
	private final VerificationMailSender verificationMailSender;

	@Transactional
	public EmailVerificationSendResponse send(String email, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();

		// 엔티티를 로딩하기 전에 정리한다. 벌크 삭제를 나중에 부르면 영속 상태가 꼬인다.
		// 기준 시각을 한도 집계 구간보다 앞에 둬야 최근 발송 이력이 지워지지 않는다.
		emailVerificationRepository.deleteDeadRows(email, now.minus(RATE_LIMIT_WINDOW));

		emailVerificationRepository.findFirstByEmailAndPurposeOrderByIdDesc(email, purpose)
			.ifPresent(latest -> {
				if (latest.getCreatedAt().plus(properties.resendCooldown()).isAfter(now)) {
					throw new BaseException(AuthErrorCode.VERIFICATION_RESEND_TOO_SOON);
				}
				latest.invalidate(now);
			});

		long sentInWindow = emailVerificationRepository.countByEmailAndPurposeAndCreatedAtAfter(
			email, purpose, now.minus(RATE_LIMIT_WINDOW)
		);
		if (sentInWindow >= properties.maxSendPerHour()) {
			throw new BaseException(AuthErrorCode.VERIFICATION_SEND_LIMIT_EXCEEDED);
		}

		String code = generateCode();
		LocalDateTime expiresAt = now.plus(properties.codeValidity());
		emailVerificationRepository.save(EmailVerification.issue(email, purpose, hash(code), expiresAt));

		// 계정이 없는 비밀번호 재설정 요청은 메일만 보내지 않는다. 응답은 동일하게 성공이다.
		// 여기서 실패를 알리면 아무 이메일이나 넣어보고 가입 여부를 알아낼 수 있다.
		if (shouldDeliver(email, purpose)) {
			sendAfterCommit(email, purpose, code);
		}

		return new EmailVerificationSendResponse(expiresAt, now.plus(properties.resendCooldown()));
	}

	// 실패해도 시도 횟수 증가와 코드 무효화는 남아야 한다. 롤백되면 무제한 대입이 가능해진다.
	@Transactional(noRollbackFor = BaseException.class)
	public EmailVerificationConfirmResponse confirm(String email, String code, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();

		EmailVerification verification = emailVerificationRepository
			.findFirstByEmailAndPurposeOrderByIdDesc(email, purpose)
			.orElseThrow(() -> new BaseException(AuthErrorCode.INVALID_VERIFICATION_CODE));

		// 이미 인증이 끝난 코드로 다시 눌러도 같은 결과를 준다.
		if (verification.isVerificationAlive(now, properties.verifiedValidity())) {
			return new EmailVerificationConfirmResponse(verification.verifiedUntil(properties.verifiedValidity()));
		}

		if (verification.isExpired(now)
			|| verification.isConsumed()
			|| !verification.hasAttemptsLeft(properties.maxAttempts())) {
			throw new BaseException(AuthErrorCode.INVALID_VERIFICATION_CODE);
		}

		verification.increaseAttemptCount();

		if (!verification.matches(hash(code))) {
			if (!verification.hasAttemptsLeft(properties.maxAttempts())) {
				verification.invalidate(now);
			}
			throw new BaseException(AuthErrorCode.INVALID_VERIFICATION_CODE);
		}

		verification.markVerified(now);

		return new EmailVerificationConfirmResponse(verification.verifiedUntil(properties.verifiedValidity()));
	}

	// 회원가입·비밀번호 재설정이 인증을 소진한다. 같은 인증으로 두 번 처리되지 않도록 막는다.
	@Transactional
	public void consumeVerified(String email, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();

		EmailVerification verification = emailVerificationRepository
			.findFirstByEmailAndPurposeOrderByIdDesc(email, purpose)
			.filter(it -> it.isVerificationAlive(now, properties.verifiedValidity()))
			.orElseThrow(() -> new BaseException(AuthErrorCode.EMAIL_NOT_VERIFIED));

		verification.markConsumed(now);
	}

	private boolean shouldDeliver(String email, VerificationPurpose purpose) {
		if (purpose != VerificationPurpose.PASSWORD_RESET) {
			return true;
		}

		return userRepository.findByEmail(email).isPresent();
	}

	// 커밋 전에 보내면 롤백된 인증번호가 사용자에게 도착한다. 입력해도 실패하는 코드다.
	private void sendAfterCommit(String email, VerificationPurpose purpose, String code) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			verificationMailSender.sendVerificationCode(email, purpose, code);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				verificationMailSender.sendVerificationCode(email, purpose, code);
			}
		});
	}

	private String generateCode() {
		return "%06d".formatted(SECURE_RANDOM.nextInt(CODE_BOUND));
	}

	private String hash(String code) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

}
