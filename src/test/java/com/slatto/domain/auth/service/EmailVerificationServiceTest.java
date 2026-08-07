package com.slatto.domain.auth.service;

import com.slatto.domain.auth.entity.EmailVerification;
import com.slatto.domain.auth.enums.VerificationPurpose;
import com.slatto.domain.auth.exception.AuthErrorCode;
import com.slatto.domain.auth.repository.EmailVerificationRepository;
import com.slatto.global.config.properties.EmailVerificationProperties;
import com.slatto.global.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(EmailVerificationService.class)
@EnableConfigurationProperties(EmailVerificationProperties.class)
@TestPropertySource(properties = {
	"spring.jpa.database=h2",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"app.email-verification.code-validity=PT5M",
	"app.email-verification.resend-cooldown=PT1M",
	"app.email-verification.verified-validity=PT30M",
	"app.email-verification.max-send-per-hour=5",
	"app.email-verification.max-attempts=5"
})
class EmailVerificationServiceTest {

	private static final String EMAIL = "tester@slatto.com";
	private static final String WRONG_CODE = "000000";
	private static final int MAX_ATTEMPTS = 5;

	// 실제 SMTP 발송을 붙이지 않는다. 발송 자체는 커밋 이후에 일어나 이 슬라이스에서는 검증 대상이 아니다.
	@MockitoBean
	private VerificationMailSender verificationMailSender;

	@Autowired
	private EmailVerificationService emailVerificationService;

	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	@Test
	@DisplayName("재발송 쿨다운 안에 다시 요청하면 거부한다")
	void rejectsResendWithinCooldown() {
		emailVerificationService.send(EMAIL, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> emailVerificationService.send(EMAIL, VerificationPurpose.SIGNUP))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.VERIFICATION_RESEND_TOO_SOON);
	}

	// 확인 실패는 예외를 던지지만 시도 횟수는 남아야 한다. 롤백되면 무제한 대입이 가능해진다.
	@Test
	@DisplayName("인증번호가 틀리면 실패해도 시도 횟수가 남는다")
	void keepsAttemptCountOnFailure() {
		emailVerificationService.send(EMAIL, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, WRONG_CODE, VerificationPurpose.SIGNUP))
			.isInstanceOf(BaseException.class);

		assertThat(latest().getAttemptCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("시도 횟수를 모두 소진하면 코드가 무효화된다")
	void invalidatesCodeAfterMaxAttempts() {
		emailVerificationService.send(EMAIL, VerificationPurpose.SIGNUP);

		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, WRONG_CODE, VerificationPurpose.SIGNUP))
				.isInstanceOf(BaseException.class);
		}

		EmailVerification verification = latest();
		assertThat(verification.getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
		assertThat(verification.isExpired(LocalDateTime.now())).isTrue();
	}

	@Test
	@DisplayName("발송 이력이 없는 이메일의 인증번호 확인은 실패한다")
	void rejectsConfirmWithoutSentCode() {
		assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, WRONG_CODE, VerificationPurpose.SIGNUP))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);
	}

	@Test
	@DisplayName("인증을 마치지 않은 이메일은 소진할 수 없다")
	void rejectsConsumeWithoutVerification() {
		emailVerificationService.send(EMAIL, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> emailVerificationService.consumeVerified(EMAIL, VerificationPurpose.SIGNUP))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.EMAIL_NOT_VERIFIED);
	}

	private EmailVerification latest() {
		return emailVerificationRepository
			.findFirstByEmailAndPurposeOrderByIdDesc(EMAIL, VerificationPurpose.SIGNUP)
			.orElseThrow();
	}

}
