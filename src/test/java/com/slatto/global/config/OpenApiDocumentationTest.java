package com.slatto.global.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
	private Map<String, Object> actualResponse(CommonErrorCode errorCode) {
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

}
