package com.slatto.global.config;

import com.slatto.global.response.code.BaseCode;
import com.slatto.global.response.code.ErrorCodeRegistry;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link ApiErrorCodes} 에 적힌 도메인 에러 응답을 문서에 붙인다.
 *
 * <p>도메인 에러는 규칙에서만 나오기 때문에 공통 에러처럼 조건으로 추론할 수 없다.
 * 엔드포인트가 직접 밝힌 것만 싣는다.
 *
 * <p>{@code OperationCustomizer} 로 따로 등록하지 않고 {@link SwaggerErrorResponseCustomizer} 가 마지막에 부른다.
 * 공통 응답이 먼저 깔린 뒤에 얹혀야 같은 상태 코드에서 공통 예시를 밀어내지 않는데,
 * springdoc 이 커스터마이저를 부르는 순서는 {@code @Order} 로 정해지지 않기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class DomainErrorResponses {

	private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
	private static final String CODE_FIELD = "code";
	private static final String FALLBACK_EXAMPLE_NAME = "기본";
	private static final String DESCRIPTION_DELIMITER = " / ";

	private final ErrorCodeRegistry errorCodeRegistry;

	public void apply(Operation operation, HandlerMethod handlerMethod) {
		ApiErrorCodes declared = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
		ApiResponses responses = operation.getResponses();

		if (declared == null || responses == null) {
			return;
		}

		groupByStatus(declared.value()).forEach((status, errorCodes) -> merge(responses, status, errorCodes));
	}

	// 한 상태 코드에 응답 객체는 하나뿐이다. 여러 도메인 코드가 같은 상태를 쓰면 예시로 나눠 담아야 한다.
	private Map<String, List<BaseCode>> groupByStatus(String[] codes) {
		Map<String, List<BaseCode>> grouped = new LinkedHashMap<>();

		for (String code : codes) {
			BaseCode errorCode = errorCodeRegistry.find(code);
			String status = String.valueOf(errorCode.getHttpStatus().value());

			grouped.computeIfAbsent(status, key -> new ArrayList<>()).add(errorCode);
		}

		return grouped;
	}

	private void merge(ApiResponses responses, String status, List<BaseCode> errorCodes) {
		ApiResponse response = responses.get(status);

		if (response == null) {
			response = new ApiResponse().description(describe(errorCodes));
			responses.addApiResponse(status, response);
		}

		if (response.getContent() == null) {
			response.setContent(new Content());
		}

		MediaType mediaType = response.getContent().get(JSON);

		if (mediaType == null) {
			mediaType = new MediaType().schema(ErrorResponseExamples.schemaRef());
			response.getContent().addMediaType(JSON, mediaType);
		}

		moveSingleExampleIntoExamples(mediaType);

		for (BaseCode errorCode : errorCodes) {
			mediaType.addExamples(errorCode.getCode(), new Example()
				.summary(errorCode.getMessage())
				.value(ErrorResponseExamples.of(errorCode)));
		}
	}

	// OpenAPI 는 example 과 examples 가 함께 있으면 example 을 버린다.
	// 공통 응답에 들어 있는 단일 예시를 그대로 두면 도메인 예시를 얹는 순간 사라진다.
	private void moveSingleExampleIntoExamples(MediaType mediaType) {
		Object example = mediaType.getExample();

		if (example == null || mediaType.getExamples() != null) {
			return;
		}

		mediaType.addExamples(exampleName(example), new Example().value(example));

		// setExample(null) 만으로는 지워지지 않는다. 값을 넣은 적이 있다는 표시가 남아 example: null 이 그대로 실린다.
		mediaType.setExample(null);
		mediaType.setExampleSetFlag(false);
	}

	private String exampleName(Object example) {
		if (example instanceof Map<?, ?> body && body.get(CODE_FIELD) instanceof String code) {
			return code;
		}

		return FALLBACK_EXAMPLE_NAME;
	}

	private String describe(List<BaseCode> errorCodes) {
		return errorCodes.stream()
			.map(BaseCode::getMessage)
			.distinct()
			.collect(Collectors.joining(DESCRIPTION_DELIMITER));
	}
}
