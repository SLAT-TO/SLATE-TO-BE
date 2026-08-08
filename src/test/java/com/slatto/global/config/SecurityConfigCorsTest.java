package com.slatto.global.config;

import com.slatto.global.config.properties.CorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

	private static final String CONTROLLER_PACKAGE = "com.slatto";

	private final CorsConfiguration configuration = corsConfiguration();

	@Test
	@DisplayName("컨트롤러가 쓰는 HTTP 메서드는 모두 CORS 허용 목록에 있다")
	void allowsEveryHttpMethodUsedByControllers() {
		Set<String> declaredMethods = httpMethodsDeclaredByControllers();

		assertThat(declaredMethods).isNotEmpty();
		assertThat(configuration.getAllowedMethods()).containsAll(declaredMethods);
	}

	@Test
	@DisplayName("허용 오리진의 프리플라이트는 요청 메서드를 그대로 돌려준다")
	void permitsPreflightForAllowedOrigin() {
		assertThat(configuration.checkOrigin("https://slatto.site")).isEqualTo("https://slatto.site");
		assertThat(configuration.checkHttpMethod(org.springframework.http.HttpMethod.PUT)).isNotNull();
	}

	@Test
	@DisplayName("허용 목록에 없는 오리진은 거부한다")
	void rejectsUnknownOrigin() {
		assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
	}

	private CorsConfiguration corsConfiguration() {
		SecurityConfig securityConfig = new SecurityConfig(
			null,
			null,
			null,
			new CorsProperties(List.of("https://slatto.site", "http://localhost:3000"))
		);

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/users/me/profile-image");

		return securityConfig.corsConfigurationSource().getCorsConfiguration(request);
	}

	// 컨트롤러에 매핑된 메서드를 실제로 훑어서, 새 HTTP 메서드가 추가되면 이 테스트가 먼저 깨지게 한다.
	private Set<String> httpMethodsDeclaredByControllers() {
		ClassPathScanningCandidateComponentProvider scanner =
			new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

		Set<String> httpMethods = new LinkedHashSet<>();

		for (BeanDefinition definition : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
			Class<?> controller = resolve(definition.getBeanClassName());

			for (Method method : controller.getDeclaredMethods()) {
				RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

				if (mapping == null) {
					continue;
				}

				Arrays.stream(mapping.method())
					.map(Enum::name)
					.forEach(httpMethods::add);
			}
		}

		return httpMethods;
	}

	private Class<?> resolve(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException(className + " 를 로드할 수 없습니다.", exception);
		}
	}

}
