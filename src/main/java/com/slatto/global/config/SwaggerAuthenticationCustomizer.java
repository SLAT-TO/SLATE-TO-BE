package com.slatto.global.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

/**
 * 토큰이 선택인 엔드포인트를 문서에 그대로 드러낸다.
 *
 * <p>문서 전체에 인증 요구가 걸려 있어서 기본값은 "토큰 필수"다.
 * 게스트 참여 경로는 토큰 없이도 되고 있어도 되는데, 이 상태를 표현할 자리가 없어
 * 필수처럼 읽히거나 아예 로그인 사용자를 받지 않는 것처럼 읽힌다.
 *
 * <p>OpenAPI 는 security 목록에 빈 요구사항을 함께 넣으면 둘 다 허용이라는 뜻이 된다.
 * 그 표현을 쓰고, 토큰 유무에 따라 무엇이 달라지는지는 설명으로 덧붙인다.
 */
@Component
public class SwaggerAuthenticationCustomizer implements OperationCustomizer {

	private static final String BEARER_AUTH = "bearerAuth";
	private static final String OPTIONAL_AUTH_NOTE =
		"인증은 선택입니다. 토큰을 보내면 로그인 사용자로, 보내지 않으면 게스트로 처리됩니다.";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		if (!EndpointAuthentication.isOptional(handlerMethod)) {
			return operation;
		}

		// 빈 요구사항이 "인증 없이도 허용"을 뜻한다. 앞의 항목과 함께 두면 둘 다 받는다는 의미가 된다.
		operation.setSecurity(List.of(
			new SecurityRequirement().addList(BEARER_AUTH),
			new SecurityRequirement()
		));

		operation.setDescription(appendNote(operation.getDescription()));

		return operation;
	}

	// 엔드포인트가 이미 적어 둔 설명을 지우지 않는다.
	private String appendNote(String description) {
		if (description == null || description.isBlank()) {
			return OPTIONAL_AUTH_NOTE;
		}

		if (description.contains(OPTIONAL_AUTH_NOTE)) {
			return description;
		}

		return description + "\n\n" + OPTIONAL_AUTH_NOTE;
	}

}
