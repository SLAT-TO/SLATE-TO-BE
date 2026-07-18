package com.slatto.domain.auth.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {

	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "리프레시 토큰이 만료되었거나 유효하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public boolean isSuccess() {
		return false;
	}

}
