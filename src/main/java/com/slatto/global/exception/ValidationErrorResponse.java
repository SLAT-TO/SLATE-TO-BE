package com.slatto.global.exception;

import java.util.List;
import org.springframework.validation.FieldError;

public record ValidationErrorResponse(
	List<FieldErrorDetail> errors
) {

	public static ValidationErrorResponse from(List<FieldError> fieldErrors) {
		List<FieldErrorDetail> errors = fieldErrors.stream()
			.map(FieldErrorDetail::from)
			.toList();

		return new ValidationErrorResponse(errors);
	}

	public record FieldErrorDetail(
		String field,
		String reason
	) {

		private static FieldErrorDetail from(FieldError fieldError) {
			return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
		}

	}

}
