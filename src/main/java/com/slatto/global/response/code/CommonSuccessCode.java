package com.slatto.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonSuccessCode implements BaseCode {

	OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),
	CREATED(HttpStatus.CREATED, "COMMON201", "생성에 성공했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public boolean isSuccess() {
		return true;
	}

}
