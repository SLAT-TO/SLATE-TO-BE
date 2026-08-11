package com.slatto.global.config;

import com.slatto.global.response.code.BaseCode;
import io.swagger.v3.oas.models.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 실패 응답 예시를 만든다.
 *
 * <p>공통 에러와 도메인 에러를 서로 다른 커스터마이저가 붙이는데,
 * 예시 모양이 두 곳에서 갈라지면 같은 상태 코드 안에 형태가 다른 예시가 나란히 실린다.
 */
final class ErrorResponseExamples {

	private ErrorResponseExamples() {
	}

	// 예시를 손으로 적으면 코드나 메시지가 바뀔 때 문서만 조용히 낡는다. enum 에서 그대로 가져온다.
	static Map<String, Object> of(BaseCode errorCode) {
		Map<String, Object> example = new LinkedHashMap<>();
		example.put("isSuccess", false);
		example.put("code", errorCode.getCode());
		example.put("message", errorCode.getMessage());
		example.put("result", null);

		return example;
	}

	static Schema<?> schemaRef() {
		return new Schema<>().$ref(SwaggerConfig.ERROR_RESPONSE_SCHEMA_REF);
	}
}
