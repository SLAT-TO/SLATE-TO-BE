package com.slatto.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.validation.FieldError;

@Schema(description = "요청 필드 검증에 실패했을 때 공통 응답의 result 에 담기는 본문")
public record ValidationErrorResponse(
	@Schema(description = "검증에 실패한 필드 목록")
	List<FieldErrorDetail> errors
) {

	public static ValidationErrorResponse from(List<FieldError> fieldErrors) {
		List<FieldErrorDetail> errors = fieldErrors.stream()
			.map(FieldErrorDetail::from)
			.toList();

		return new ValidationErrorResponse(errors);
	}

	@Schema(description = "필드 단위 검증 실패 내용")
	public record FieldErrorDetail(
		@Schema(description = "검증에 실패한 필드명", example = "title")
		String field,

		@Schema(description = "실패 사유", example = "공백일 수 없습니다")
		String reason
	) {

		private static FieldErrorDetail from(FieldError fieldError) {
			return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
		}

	}

}
