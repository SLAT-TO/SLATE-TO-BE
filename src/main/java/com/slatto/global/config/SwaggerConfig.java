package com.slatto.global.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.global.exception.ValidationErrorResponse;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonErrorCode;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SwaggerConfig {

	public static final String ERROR_RESPONSE_SCHEMA = "ErrorResponse";
	public static final String ERROR_RESPONSE_SCHEMA_REF = "#/components/schemas/" + ERROR_RESPONSE_SCHEMA;

	private static final String VALIDATION_ERROR_SCHEMA = "ValidationErrorResponse";
	private static final String VALIDATION_ERROR_SCHEMA_REF = "#/components/schemas/" + VALIDATION_ERROR_SCHEMA;
	private static final String RESULT_PROPERTY = "result";
	private static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI openAPI(ObjectMapper objectMapper) {
		Components components = new Components()
			.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT"));

		registerErrorSchemas(components, objectMapper);

		return new OpenAPI()
			.info(new Info()
				.title("SLAT-TO Backend API")
				.description("SLAT-TO backend API documentation")
				.version("v1"))
			.components(components)
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

	// 실패 응답 스키마는 실제 응답 클래스에서 뽑아낸다.
	// 손으로 필드를 적어두면 응답 클래스가 바뀌었을 때 문서만 조용히 낡는다.
	private void registerErrorSchemas(Components components, ObjectMapper objectMapper) {
		ModelConverters.getInstance()
			.readAllAsResolvedSchema(new AnnotatedType(ValidationErrorResponse.class))
			.referencedSchemas
			.forEach(components::addSchemas);

		Schema<?> errorResponse = resolveSchema(ApiResponse.class)
			.description("실패 응답. 성공과 동일한 래퍼를 사용한다.");

		applyFailureExamples(errorResponse, objectMapper);

		// 래퍼의 result 는 제네릭이라 object 로만 잡힌다.
		// 실패에서 result 에 들어갈 수 있는 유일한 본문을 직접 가리켜서, 검증 실패 구조가 문서에 드러나게 한다.
		// 참조되지 않는 스키마는 springdoc 이 문서에서 걷어내므로 이 연결이 곧 등록 조건이기도 하다.
		errorResponse.getProperties().put(RESULT_PROPERTY, new Schema<>()
			.$ref(VALIDATION_ERROR_SCHEMA_REF)
			.nullable(true)
			.description("검증 실패에서만 필드 오류 목록이 담기고, 그 외에는 null 이다."));

		components.addSchemas(ERROR_RESPONSE_SCHEMA, errorResponse);
	}

	// 래퍼에 붙은 예시는 성공 기준이라 실패 스키마에 그대로 쓰면 문서가 거짓말을 한다.
	// 실제 실패 응답을 직렬화해서 덮어쓴다.
	private void applyFailureExamples(Schema<?> schema, ObjectMapper objectMapper) {
		Map<String, Object> failure = objectMapper.convertValue(
			ApiResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR),
			new TypeReference<>() {
			}
		);

		schema.getProperties().forEach((name, property) -> property.setExample(failure.get(name)));
	}

	private Schema<?> resolveSchema(Class<?> type) {
		ResolvedSchema resolved = ModelConverters.getInstance()
			.readAllAsResolvedSchema(new AnnotatedType(type));
		Schema<?> named = resolved.referencedSchemas.get(type.getSimpleName());

		return named != null ? named : resolved.schema;
	}

}
