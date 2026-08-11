package com.slatto.global.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.method.HandlerMethod;

/**
 * 엔드포인트가 문서상 어떤 인증을 요구하는지 판정한다.
 *
 * <p>Swagger 커스터마이저 두 곳에서 같은 판정을 쓰기 때문에 여기 모아 둔다.
 * 판정 기준이 갈라지면 자물쇠 표시와 401 문서화가 서로 어긋난다.
 */
final class EndpointAuthentication {

	private EndpointAuthentication() {
	}

	/**
	 * 토큰 없이 호출하는 것이 정상인 경로. 비어 있는 {@code @SecurityRequirements} 로 표시한다.
	 */
	static boolean isAnonymous(HandlerMethod handlerMethod) {
		return handlerMethod.hasMethodAnnotation(SecurityRequirements.class);
	}

	/**
	 * 토큰이 선택인 경로. {@link OptionalAuthentication} 으로 표시한다.
	 */
	static boolean isOptional(HandlerMethod handlerMethod) {
		return handlerMethod.hasMethodAnnotation(OptionalAuthentication.class);
	}

	/**
	 * 토큰이 없으면 401 로 막히는 경로. 표시가 하나도 없으면 여기에 해당한다.
	 */
	static boolean isRequired(HandlerMethod handlerMethod) {
		return !isAnonymous(handlerMethod) && !isOptional(handlerMethod);
	}

}
