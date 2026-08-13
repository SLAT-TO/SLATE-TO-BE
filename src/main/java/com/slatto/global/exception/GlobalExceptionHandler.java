package com.slatto.global.exception;

import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.BaseCode;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		MissingRequestHeaderException.class,
		HandlerMethodValidationException.class,
		ConstraintViolationException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception) {
		CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	// 본문이나 multipart 파트의 Content-Type 이 컨트롤러가 받을 수 있는 형식이 아닐 때다.
	// 클라이언트가 보낸 형식의 문제인데, 여기서 잡지 않으면 handleUnexpectedException 이
	// 받아 COMMON500 을 내보내 서버 장애처럼 보인다.
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaTypeException(
		HttpMediaTypeNotSupportedException exception
	) {
		CommonErrorCode errorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
		log.warn("[MediaType] 지원하지 않는 요청 형식입니다. contentType={}", exception.getContentType());

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

	// multipart 한도 초과는 요청 본문을 읽는 단계에서 터진다. 컨트롤러에 닿지 않으니
	// 서비스의 파일 크기 검증(PROJECT_FILE_SIZE400 등)은 실행조차 되지 않는다.
	// 여기서 잡지 않으면 handleUnexpectedException 이 받아 COMMON500 을 내보내고,
	// 프론트는 "파일이 너무 큽니다" 대신 서버 오류를 표시하게 된다.
	// 앞단 nginx 도 한도를 넘기면 413 을 주므로 상태 코드를 413 으로 맞춰 프론트가 한 갈래로 처리하게 한다.
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handlePayloadTooLargeException(
		MaxUploadSizeExceededException exception
	) {
		CommonErrorCode errorCode = CommonErrorCode.PAYLOAD_TOO_LARGE;
		log.warn("[Multipart] 업로드 한도를 초과한 요청입니다. maxUploadSize={}", exception.getMaxUploadSize());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	// 한도 초과 외의 multipart 해석 실패(본문이 잘림, boundary 불일치 등)는 클라이언트 요청 문제다.
	// 서버 오류로 올리면 업로드 중 연결이 끊길 때마다 500 로그가 쌓인다.
	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException exception) {
		CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;
		log.warn("[Multipart] 요청을 해석하지 못했습니다.", exception);

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("[Unhandled Exception] ", exception);
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode));
	}

}
