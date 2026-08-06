package com.slatto.domain.auth.service;

import com.slatto.domain.auth.enums.VerificationPurpose;
import com.slatto.global.config.AsyncConfig;
import com.slatto.global.config.properties.MailSenderProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

	private final JavaMailSender javaMailSender;
	private final MailSenderProperties mailSenderProperties;

	// 비동기라 API 는 이미 성공 응답을 보낸 뒤다. 실패를 사용자에게 알릴 수단이 없으므로
	// 로그만 남기고 재발송으로 갈음한다. 예외를 던져도 받아줄 곳이 없다.
	@Async(AsyncConfig.MAIL_EXECUTOR)
	public void sendVerificationCode(String email, VerificationPurpose purpose, String code) {
		try {
			MimeMessage message = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

			helper.setFrom(new InternetAddress(
				mailSenderProperties.fromAddress(),
				mailSenderProperties.fromName(),
				StandardCharsets.UTF_8.name()
			));
			helper.setTo(email);
			helper.setSubject(resolveSubject(purpose));
			helper.setText(resolveBody(purpose, code), false);

			javaMailSender.send(message);
		} catch (Exception exception) {
			log.warn("[Mail] 인증번호 발송 실패. email={}, purpose={}", email, purpose, exception);
		}
	}

	private String resolveSubject(VerificationPurpose purpose) {
		return purpose == VerificationPurpose.PASSWORD_RESET
			? "[슬레이투] 비밀번호 재설정 인증번호"
			: "[슬레이투] 회원가입 인증번호";
	}

	private String resolveBody(VerificationPurpose purpose, String code) {
		String action = purpose == VerificationPurpose.PASSWORD_RESET ? "비밀번호 재설정" : "회원가입";

		return """
			안녕하세요, 슬레이투입니다.

			%s을 위한 인증번호는 다음과 같습니다.

			%s

			인증번호는 5분간 유효합니다.
			본인이 요청하지 않았다면 이 메일을 무시해 주세요.
			""".formatted(action, code);
	}

}
