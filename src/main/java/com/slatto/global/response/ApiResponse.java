package com.slatto.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slatto.global.response.code.BaseCode;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

	@JsonProperty("isSuccess")
	private final boolean success;

	private final String code;

	private final String message;

	private final T result;

	private ApiResponse(BaseCode baseCode, T result) {
		this.success = baseCode.isSuccess();
		this.code = baseCode.getCode();
		this.message = baseCode.getMessage();
		this.result = result;
	}

	public static <T> ApiResponse<T> success(BaseCode baseCode, T result) {
		return new ApiResponse<>(baseCode, result);
	}

	public static ApiResponse<Void> failure(BaseCode baseCode) {
		return new ApiResponse<>(baseCode, null);
	}

	public static <T> ApiResponse<T> failure(BaseCode baseCode, T result) {
		return new ApiResponse<>(baseCode, result);
	}

}
