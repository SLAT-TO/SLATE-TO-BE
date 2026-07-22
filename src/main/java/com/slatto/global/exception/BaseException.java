package com.slatto.global.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

	private final BaseCode errorCode;

	public BaseException(BaseCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

}
