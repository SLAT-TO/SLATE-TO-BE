package com.slatto.global.config;

import com.slatto.global.response.code.CommonErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통 에러 응답을 모든 엔드포인트 문서에 붙인다.
 *
 * <p>에러는 {@code GlobalExceptionHandler} 가 전역에서 처리하기 때문에 컨트롤러에는 흔적이 남지 않는다.
 * 그래서 문서를 생성하면 성공 응답만 노출되고, 실제로는 존재하는 실패 응답이 명세에서 통째로 사라진다.
 * 엔드포인트마다 손으로 적는 대신 여기서 한 번에 주입한다.
 *
 * <p>실제로 발생할 수 있는 상태 코드만 붙인다. 문서에 있는 상태 코드가 실제로 나지 않으면
 * 명세와 구현이 어긋난 것과 같기 때문에, 조건 없이 전부 붙이지 않는다.
 */
@Component
public class SwaggerErrorResponseCustomizer implements OperationCustomizer {

	private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
	private static final String PATH_PARAMETER = "path";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		ApiResponses responses = operation.getResponses();

		if (responses == null) {
			return operation;
		}

		// 400 은 파싱할 본문이나 파라미터가 있어야 발생한다. 둘 다 없으면 날 수 없다.
		if (hasParameters(operation) || operation.getRequestBody() != null) {
			addIfAbsent(responses, CommonErrorCode.BAD_REQUEST, badRequestContent());
		}

		// 401 은 토큰이 필수인 엔드포인트에서만 발생한다.
		// 게스트 참여 경로는 토큰이 없어도 통과하기 때문에 여기서 제외된다.
		if (EndpointAuthentication.isRequired(handlerMethod)) {
			addIfAbsent(responses, CommonErrorCode.UNAUTHORIZED, singleExampleContent(CommonErrorCode.UNAUTHORIZED));
		}

		// 404 는 경로로 리소스를 찾는 엔드포인트에서만 발생한다.
		if (hasPathParameter(operation)) {
			addIfAbsent(responses, CommonErrorCode.NOT_FOUND, singleExampleContent(CommonErrorCode.NOT_FOUND));
		}

		// 500 은 처리되지 않은 예외를 잡는 핸들러가 있어 모든 엔드포인트에서 가능하다.
		addIfAbsent(
			responses,
			CommonErrorCode.INTERNAL_SERVER_ERROR,
			singleExampleContent(CommonErrorCode.INTERNAL_SERVER_ERROR)
		);

		return operation;
	}

	// 엔드포인트가 직접 선언한 응답이 우선한다. 여기서 덮어쓰면 개별 문서화가 무의미해진다.
	private void addIfAbsent(ApiResponses responses, CommonErrorCode errorCode, Content content) {
		String status = String.valueOf(errorCode.getHttpStatus().value());

		if (responses.containsKey(status)) {
			return;
		}

		responses.addApiResponse(status, new ApiResponse()
			.description(errorCode.getMessage())
			.content(content));
	}

	private Content badRequestContent() {
		MediaType mediaType = new MediaType().schema(errorSchemaRef());

		mediaType.addExamples("검증 실패", new Example()
			.summary("요청 필드가 검증 조건을 만족하지 못한 경우")
			.value(validationFailureExample()));

		mediaType.addExamples("잘못된 요청", new Example()
			.summary("본문 파싱 실패, 타입 불일치, 필수 파라미터 누락")
			.value(errorExample(CommonErrorCode.BAD_REQUEST)));

		return new Content().addMediaType(JSON, mediaType);
	}

	private Content singleExampleContent(CommonErrorCode errorCode) {
		return new Content().addMediaType(JSON, new MediaType()
			.schema(errorSchemaRef())
			.example(errorExample(errorCode)));
	}

	private Schema<?> errorSchemaRef() {
		return new Schema<>().$ref(SwaggerConfig.ERROR_RESPONSE_SCHEMA_REF);
	}

	// 예시를 손으로 적으면 코드나 메시지가 바뀔 때 문서만 조용히 낡는다. enum 에서 그대로 가져온다.
	private Map<String, Object> errorExample(CommonErrorCode errorCode) {
		Map<String, Object> example = new LinkedHashMap<>();
		example.put("isSuccess", false);
		example.put("code", errorCode.getCode());
		example.put("message", errorCode.getMessage());
		example.put("result", null);

		return example;
	}

	private Map<String, Object> validationFailureExample() {
		Map<String, Object> fieldError = new LinkedHashMap<>();
		fieldError.put("field", "title");
		fieldError.put("reason", "공백일 수 없습니다");

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("errors", List.of(fieldError));

		Map<String, Object> example = errorExample(CommonErrorCode.BAD_REQUEST);
		example.put("result", result);

		return example;
	}

	private boolean hasParameters(Operation operation) {
		List<Parameter> parameters = operation.getParameters();

		return parameters != null && !parameters.isEmpty();
	}

	private boolean hasPathParameter(Operation operation) {
		List<Parameter> parameters = operation.getParameters();

		if (parameters == null) {
			return false;
		}

		return parameters.stream().anyMatch(parameter -> PATH_PARAMETER.equals(parameter.getIn()));
	}

}
