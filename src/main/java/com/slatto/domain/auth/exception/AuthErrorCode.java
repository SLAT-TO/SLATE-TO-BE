package com.slatto.domain.auth.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {

	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "리프레시 토큰이 만료되었거나 유효하지 않습니다."),

	// 인증번호 실패 사유(불일치·만료·시도 초과)를 구분하지 않는다. 세분화하면 공격자에게 힌트가 된다.
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_VERIFICATION_CODE400", "인증번호가 올바르지 않거나 만료되었습니다."),
	VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "AUTH_VERIFICATION_RESEND429", "잠시 후 다시 시도해 주세요."),
	VERIFICATION_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_VERIFICATION_LIMIT429", "인증번호 발송 횟수를 초과했습니다. 1시간 후 다시 시도해 주세요."),
	EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_EMAIL_NOT_VERIFIED400", "이메일 인증이 완료되지 않았습니다."),
	SIGNUP_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_SIGNUP_DUPLICATE409", "이미 가입된 이메일입니다."),
	SIGNUP_SOCIAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "AUTH_SIGNUP_SOCIAL409", "구글 계정으로 가입된 이메일입니다. 구글 로그인을 이용해 주세요."),

	// 이메일 미존재·비밀번호 불일치·소셜 전용 계정을 모두 같은 응답으로 처리한다. 이메일 열거 방지다.
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN401", "이메일 또는 비밀번호가 올바르지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public boolean isSuccess() {
		return false;
	}

}
