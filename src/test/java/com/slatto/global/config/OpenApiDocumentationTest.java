package com.slatto.global.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.BaseCode;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.response.code.ErrorCodeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 실제로 생성되는 OpenAPI 문서를 검증한다.
 *
 * <p>문서는 애노테이션에서 조립되기 때문에 애노테이션을 빠뜨려도 빌드가 깨지지 않는다.
 * 그래서 누락은 배포 후에야 드러난다. 생성 결과를 직접 확인해서 먼저 깨지게 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

	private static final Set<String> HTTP_METHODS =
		Set.of("get", "post", "put", "patch", "delete", "options", "head", "trace");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ErrorCodeRegistry errorCodeRegistry;

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	private JsonNode apiDocs;

	@BeforeEach
	void fetchApiDocs() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		apiDocs = objectMapper.readTree(body);
	}

	@Test
	@DisplayName("문서에 노출된 모든 엔드포인트는 설명을 가진다")
	void everyExposedOperationHasSummary() {
		List<String> missing = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			JsonNode summary = operation.get("summary");

			if (summary == null || summary.asText().isBlank()) {
				missing.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(missing)
			.as("@Operation 이 없는 엔드포인트. 문서에서 감추려면 @Hidden 을 붙인다.")
			.isEmpty();
	}

	// summary 만 있으면 이름만 아는 상태다. 호출하는 쪽이 알아야 할 제약은 코드를 열어봐야 나온다.
	// summary 를 그대로 옮겨 적은 설명은 그 공백을 메우지 않으므로 없는 것으로 친다.
	@Test
	@DisplayName("문서에 노출된 모든 엔드포인트는 summary 를 되풀이하지 않는 설명을 가진다")
	void everyExposedOperationHasMeaningfulDescription() {
		List<String> missing = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			String description = operation.path("description").asText("").strip();
			String summary = operation.path("summary").asText("").strip();

			if (description.isBlank() || description.equals(summary)) {
				missing.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(missing)
			.as("@Operation(description = ...) 이 없는 엔드포인트")
			.isEmpty();
	}

	@Test
	@DisplayName("모든 엔드포인트는 500 응답을 문서화한다")
	void everyOperationDocumentsServerError() {
		List<String> missing = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			if (!operation.path("responses").has("500")) {
				missing.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(missing).isEmpty();
	}

	@Test
	@DisplayName("경로 변수를 받는 엔드포인트는 404 응답을 문서화한다")
	void pathVariableOperationsDocumentNotFound() {
		List<String> missing = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			if (hasPathParameter(operation) && !operation.path("responses").has("404")) {
				missing.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(missing).isEmpty();
	}

	// 413 은 업로드 한도를 서블릿 컨테이너가 강제하는 multipart 경로에서만 난다.
	// 다른 엔드포인트에 붙으면 실제로 나지 않는 상태 코드가 문서에 실린다.
	@Test
	@DisplayName("413 은 multipart 본문을 받는 엔드포인트에만 문서화된다")
	void payloadTooLargeIsDocumentedOnlyOnMultipartOperations() {
		List<String> wrong = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			boolean documentsPayloadTooLarge = operation.path("responses").has("413");

			if (consumesMultipart(operation) != documentsPayloadTooLarge) {
				wrong.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(wrong).isEmpty();
	}

	// 아래 검증은 모두 @ApiErrorCodes 를 훑는다. 애노테이션이 한 곳도 없으면 훑을 것이 없어 전부 통과한다.
	@Test
	@DisplayName("도메인 에러를 표기한 엔드포인트가 존재한다")
	void declaresDomainErrorCodesSomewhere() {
		List<String> declarations = new ArrayList<>();

		forEachDeclaration((path, httpMethod, code) -> declarations.add(httpMethod + " " + path + " → " + code));

		assertThat(declarations)
			.as("@ApiErrorCodes 로 표기된 도메인 에러")
			.isNotEmpty();
	}

	// 코드를 문자열로 적기 때문에 오타를 컴파일러가 잡지 못한다.
	// 오타가 나면 문서에 응답이 조용히 빠지므로, 여기서 해석해보고 깨뜨린다.
	@Test
	@DisplayName("@ApiErrorCodes 에 적힌 코드는 모두 실제로 존재한다")
	void declaredErrorCodesExist() {
		List<String> unknown = new ArrayList<>();

		forEachDeclaration((path, httpMethod, code) -> {
			if (!errorCodeRegistry.contains(code)) {
				unknown.add(httpMethod + " " + path + " → " + code);
			}
		});

		assertThat(unknown)
			.as("에러 코드 enum 에 없는 코드 문자열")
			.isEmpty();
	}

	// 애노테이션만 붙고 문서에 반영되지 않으면 표기해둔 의미가 없다.
	// 상태 코드와 예시가 실제 생성 결과에 있는지 확인한다.
	@Test
	@DisplayName("@ApiErrorCodes 에 적힌 코드는 문서에 해당 상태의 예시로 실린다")
	void declaredErrorCodesAppearInDocument() {
		List<String> missing = new ArrayList<>();

		forEachDeclaration((path, httpMethod, code) -> {
			BaseCode errorCode = errorCodeRegistry.find(code);
			String status = String.valueOf(errorCode.getHttpStatus().value());

			JsonNode example = apiDocs.path("paths").path(path).path(httpMethod.toLowerCase())
				.path("responses").path(status)
				.path("content").path("application/json")
				.path("examples").path(code).path("value");

			if (example.isMissingNode()) {
				missing.add(httpMethod + " " + path + " → " + status + " / " + code);
			}
		});

		assertThat(missing)
			.as("애노테이션에는 있으나 문서에 실리지 않은 도메인 에러 응답")
			.isEmpty();
	}

	// 예시 본문을 커스터마이저가 손으로 조립하기 때문에 실제 응답과 갈라질 수 있다.
	@Test
	@DisplayName("도메인 에러 예시는 실제 실패 응답 값과 일치한다")
	void domainErrorExamplesMatchActualResponse() {
		List<String> mismatched = new ArrayList<>();

		forEachDeclaration((path, httpMethod, code) -> {
			BaseCode errorCode = errorCodeRegistry.find(code);
			String status = String.valueOf(errorCode.getHttpStatus().value());

			JsonNode example = apiDocs.path("paths").path(path).path(httpMethod.toLowerCase())
				.path("responses").path(status)
				.path("content").path("application/json")
				.path("examples").path(code).path("value");

			if (example.isMissingNode()) {
				return;
			}

			Map<String, Object> documented = objectMapper.convertValue(example, new TypeReference<>() {
			});

			if (!documented.equals(actualResponse(errorCode))) {
				mismatched.add(httpMethod + " " + path + " → " + code);
			}
		});

		assertThat(mismatched).isEmpty();
	}

	// 도메인 예시를 얹을 때 공통 예시를 examples 로 옮기는데, 이 이사가 실패하면
	// OpenAPI 규칙상 example 이 무시돼 공통 실패 응답이 문서에서 사라진다.
	@Test
	@DisplayName("실패 응답은 example 과 examples 를 함께 갖지 않는다")
	void errorResponsesDoNotMixExampleAndExamples() {
		List<String> mixed = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			JsonNode responses = operation.path("responses");

			responses.fieldNames().forEachRemaining(status -> {
				JsonNode mediaType = responses.path(status).path("content").path("application/json");

				if (mediaType.has("example") && mediaType.has("examples")) {
					mixed.add(httpMethod.toUpperCase() + " " + path + " → " + status);
				}
			});
		});

		assertThat(mixed).isEmpty();
	}

	// 404 는 공통 응답과 도메인 응답이 겹치는 유일한 상태다.
	// 도메인 예시를 얹는 쪽이 공통 예시를 밀어내도 example 과 examples 가 섞이지는 않아
	// 위 검증은 통과하면서 공통 예시만 조용히 사라진다.
	//
	// 경로 변수가 있는 엔드포인트에만 공통 404 가 깔리므로, 겹침도 그쪽에서만 일어난다.
	@Test
	@DisplayName("도메인 예시를 얹은 404 도 공통 예시를 그대로 갖는다")
	void notFoundKeepsCommonExampleAlongsideDomainExamples() {
		List<String> merged = new ArrayList<>();
		List<String> dropped = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			JsonNode examples = operation.path("responses").path("404")
				.path("content").path("application/json").path("examples");

			if (examples.isMissingNode() || !hasPathParameter(operation)) {
				return;
			}

			String endpoint = httpMethod.toUpperCase() + " " + path;
			merged.add(endpoint);

			if (!examples.has(CommonErrorCode.NOT_FOUND.getCode())) {
				dropped.add(endpoint);
			}
		});

		assertThat(merged).as("공통 404 와 도메인 404 가 함께 실린 응답").isNotEmpty();
		assertThat(dropped)
			.as("도메인 예시에 밀려 공통 404 예시가 사라진 응답")
			.isEmpty();
	}

	// 401 을 일괄로 붙이면 인증 없이 열린 경로에도 발생하지 않는 상태 코드가 실린다.
	// 자물쇠 표시와 401 문서화는 항상 같은 판정에서 나와야 한다.
	@Test
	@DisplayName("401 은 인증이 필수인 엔드포인트에만 문서화된다")
	void unauthorizedIsDocumentedOnlyWhereAuthenticationIsRequired() {
		List<String> wrong = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			boolean documentsUnauthorized = operation.path("responses").has("401");

			if (requiresAuthentication(operation) != documentsUnauthorized) {
				wrong.add(httpMethod.toUpperCase() + " " + path);
			}
		});

		assertThat(wrong)
			.as("인증이 필수면 401 이 있어야 하고, 공개거나 선택이면 없어야 한다.")
			.isEmpty();
	}

	@Test
	@DisplayName("인증이 선택인 엔드포인트는 토큰 없는 호출도 허용한다고 표시한다")
	void optionalAuthenticationOperationsAllowAnonymousCalls() {
		List<String> optional = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			if (allowsAnonymous(operation) && !operation.path("security").isEmpty()) {
				optional.add(httpMethod.toUpperCase() + " " + path);

				assertThat(operation.path("description").asText())
					.as(httpMethod.toUpperCase() + " " + path + " 는 인증이 선택이라는 설명을 가진다")
					.contains("인증은 선택입니다");
			}
		});

		assertThat(optional).isNotEmpty();
	}

	@Test
	@DisplayName("실패 응답 스키마는 실제 응답 래퍼와 같은 필드를 가진다")
	void errorResponseSchemaMatchesActualWrapper() {
		Set<String> documented = fieldNames(errorResponseSchema().path("properties"));

		assertThat(documented).isEqualTo(actualResponseKeys());
	}

	private JsonNode errorResponseSchema() {
		return apiDocs.path("components").path("schemas").path("ErrorResponse");
	}

	@Test
	@DisplayName("실패 응답 스키마의 예시는 실제 실패 응답 값과 일치한다")
	void errorResponseSchemaExamplesMatchActualResponse() {
		JsonNode properties = errorResponseSchema().path("properties");
		Map<String, Object> actual = actualResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
		Map<String, Object> documented = new LinkedHashMap<>();

		properties.fieldNames().forEachRemaining(name -> {
			JsonNode example = properties.path(name).path("example");

			if (!example.isMissingNode()) {
				documented.put(name, objectMapper.convertValue(example, Object.class));
			}
		});

		assertThat(documented).isNotEmpty();
		assertThat(documented).allSatisfy((name, value) -> assertThat(value).isEqualTo(actual.get(name)));
	}

	// 참조되지 않는 스키마는 springdoc 이 문서에서 걷어낸다.
	// 연결이 끊기면 검증 실패 응답의 구조가 통째로 사라지므로 참조 자체를 고정한다.
	@Test
	@DisplayName("검증 실패 본문 스키마가 실패 응답에서 참조된다")
	void validationErrorSchemaStaysReachable() {
		JsonNode schemas = apiDocs.path("components").path("schemas");

		assertThat(errorResponseSchema().path("properties").path("result").path("$ref").asText())
			.isEqualTo("#/components/schemas/ValidationErrorResponse");
		assertThat(schemas.has("ValidationErrorResponse")).isTrue();
		assertThat(schemas.path("ValidationErrorResponse").path("properties").path("errors")
			.path("items").path("$ref").asText())
			.isEqualTo("#/components/schemas/FieldErrorDetail");
		assertThat(schemas.has("FieldErrorDetail")).isTrue();
	}

	@Test
	@DisplayName("문서의 500 예시는 실제 응답 객체와 일치한다")
	void serverErrorExampleMatchesActualResponse() {
		Map<String, Object> actual = actualResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
		List<Map<String, Object>> examples = new ArrayList<>();

		forEachOperation((path, httpMethod, operation) -> {
			JsonNode example = operation.path("responses").path("500")
				.path("content").path("application/json").path("example");

			if (!example.isMissingNode()) {
				examples.add(objectMapper.convertValue(example, new TypeReference<>() {
				}));
			}
		});

		assertThat(examples).isNotEmpty();
		assertThat(examples).allSatisfy(example -> assertThat(example).isEqualTo(actual));
	}

	// 실제 응답을 직렬화해서 비교한다. 필드명을 테스트에 적어두면 그 하드코딩도 같이 낡는다.
	private Map<String, Object> actualResponse(BaseCode errorCode) {
		return objectMapper.convertValue(ApiResponse.failure(errorCode), new TypeReference<>() {
		});
	}

	private Set<String> actualResponseKeys() {
		return new LinkedHashSet<>(actualResponse(CommonErrorCode.INTERNAL_SERVER_ERROR).keySet());
	}

	// OpenAPI 는 security 목록에 빈 요구사항이 있으면 인증 없이도 호출할 수 있다는 뜻이다.
	// 목록 자체가 비어 있으면 전역 인증 요구를 통째로 벗겨낸 공개 엔드포인트다.
	private boolean allowsAnonymous(JsonNode operation) {
		JsonNode security = operation.path("security");

		if (security.isMissingNode()) {
			return false;
		}

		if (security.isEmpty()) {
			return true;
		}

		for (JsonNode requirement : security) {
			if (requirement.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private boolean requiresAuthentication(JsonNode operation) {
		return !allowsAnonymous(operation);
	}

	private boolean consumesMultipart(JsonNode operation) {
		return fieldNames(operation.path("requestBody").path("content")).stream()
			.anyMatch(mediaType -> mediaType.startsWith("multipart/form-data"));
	}

	private boolean hasPathParameter(JsonNode operation) {
		for (JsonNode parameter : operation.path("parameters")) {
			if ("path".equals(parameter.path("in").asText())) {
				return true;
			}
		}

		return false;
	}

	private Set<String> fieldNames(JsonNode node) {
		Set<String> names = new LinkedHashSet<>();
		node.fieldNames().forEachRemaining(names::add);

		return names;
	}

	// 문서가 아니라 핸들러에서 애노테이션을 읽는다.
	// 문서에서 읽으면 "문서에 실린 것이 문서에 실렸다" 를 확인하게 된다.
	private void forEachDeclaration(DeclarationVisitor visitor) {
		handlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
			ApiErrorCodes declared = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);

			if (declared == null || mappingInfo.getPathPatternsCondition() == null) {
				return;
			}

			mappingInfo.getPathPatternsCondition().getPatterns().forEach(pattern ->
				mappingInfo.getMethodsCondition().getMethods().forEach(httpMethod -> {
					for (String code : declared.value()) {
						visitor.visit(pattern.getPatternString(), httpMethod.name(), code);
					}
				}));
		});
	}

	private void forEachOperation(OperationVisitor visitor) {
		JsonNode paths = apiDocs.path("paths");

		paths.fieldNames().forEachRemaining(path -> {
			JsonNode pathItem = paths.path(path);

			pathItem.fieldNames().forEachRemaining(httpMethod -> {
				if (HTTP_METHODS.contains(httpMethod)) {
					visitor.visit(path, httpMethod, pathItem.path(httpMethod));
				}
			});
		});
	}

	@FunctionalInterface
	private interface OperationVisitor {

		void visit(String path, String httpMethod, JsonNode operation);

	}

	@FunctionalInterface
	private interface DeclarationVisitor {

		void visit(String path, String httpMethod, String errorCode);

	}

}
