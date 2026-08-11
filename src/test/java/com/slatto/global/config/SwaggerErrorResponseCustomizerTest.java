package com.slatto.global.config;

import com.slatto.global.response.code.ErrorCodeRegistry;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공통 에러와 도메인 에러가 같은 상태 코드에서 부딪힐 때를 확인한다.
 *
 * <p>한 상태 코드에 응답 객체는 하나뿐이라, 나중에 붙는 쪽이 앞의 예시를 지워도 문서는 멀쩡해 보인다.
 * 실제 엔드포인트에 도메인 코드가 붙기 전까지는 이 충돌이 문서에 드러나지 않기 때문에 여기서 직접 부딪혀 본다.
 */
class SwaggerErrorResponseCustomizerTest {

	private final SwaggerErrorResponseCustomizer customizer =
		new SwaggerErrorResponseCustomizer(new DomainErrorResponses(new ErrorCodeRegistry()));

	@Test
	@DisplayName("같은 상태 코드에 공통 예시와 도메인 예시를 나란히 싣는다")
	void keepsCommonExampleWhenDomainCodeSharesStatus() throws NoSuchMethodException {
		MediaType mediaType = customizeNotFound();

		assertThat(mediaType.getExamples()).containsKeys("COMMON404", "PROJECT404");
	}

	// example 과 examples 가 함께 실리면 OpenAPI 는 example 을 버린다. 값을 비우는 것만으로는 표시가 남는다.
	@Test
	@DisplayName("도메인 예시를 얹으면 단일 예시 자리를 비운다")
	void clearsSingleExampleAfterMerging() throws NoSuchMethodException {
		MediaType mediaType = customizeNotFound();

		assertThat(mediaType.getExample()).isNull();
		assertThat(mediaType.getExampleSetFlag()).isFalse();
	}

	private MediaType customizeNotFound() throws NoSuchMethodException {
		Method method = Endpoint.class.getDeclaredMethod("findOne");
		Operation operation = new Operation()
			.responses(new ApiResponses())
			.addParametersItem(new Parameter().in("path").name("projectId"));

		customizer.customize(operation, new HandlerMethod(new Endpoint(), method));

		return operation.getResponses()
			.get("404")
			.getContent()
			.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
	}

	private static class Endpoint {

		@ApiErrorCodes("PROJECT404")
		void findOne() {
		}
	}
}
