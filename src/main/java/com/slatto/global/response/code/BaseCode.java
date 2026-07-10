package com.slatto.global.response.code;

import org.springframework.http.HttpStatus;

public interface BaseCode {

	boolean isSuccess();

	HttpStatus getHttpStatus();

	String getCode();

	String getMessage();

}
