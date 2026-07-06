package com.slatto.global.exception;

import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.BaseCode;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException exception) {
		BaseCode errorCode = exception.getErrorCode();

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleValidationException(
		MethodArgumentNotValidException exception
	) {
		CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;
		ValidationErrorResponse response = ValidationErrorResponse.from(exception.getFieldErrors());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode, response));
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		MissingServletRequestParameterException.class,
		HandlerMethodValidationException.class,
		ConstraintViolationException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception) {
		CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowedException(
		HttpRequestMethodNotSupportedException exception
	) {
		CommonErrorCode errorCode = CommonErrorCode.METHOD_NOT_ALLOWED;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceException(NoResourceFoundException exception) {
		CommonErrorCode errorCode = CommonErrorCode.NOT_FOUND;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

}
