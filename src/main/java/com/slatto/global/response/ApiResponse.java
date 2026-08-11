package com.slatto.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slatto.global.response.code.BaseCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@Schema(description = "모든 API 가 사용하는 공통 응답 래퍼. 성공과 실패가 같은 구조를 사용한다.")
public class ApiResponse<T> {

	@Schema(description = "요청 성공 여부", example = "true")
	@JsonProperty("isSuccess")
	private final boolean success;

	@Schema(description = "응답 코드. 도메인별 접두사와 HTTP 상태를 조합한다.", example = "COMMON200")
	private final String code;

	@Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
	private final String message;

	@Schema(description = "응답 데이터. 실패 시 null 이며, 검증 실패에서만 필드 오류 목록이 담긴다.")
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
