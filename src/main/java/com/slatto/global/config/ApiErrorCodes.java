package com.slatto.global.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 엔드포인트에서 발생할 수 있는 도메인 에러 코드를 적는다.
 *
 * <p>공통 에러(400, 401, 404, 413, 500)는 {@code SwaggerErrorResponseCustomizer} 가
 * 조건을 보고 알아서 붙이므로 여기 적지 않는다. 도메인 규칙에서만 나오는 응답을 적는 자리다.
 *
 * <p>상태 코드로 갈리지 않는다. 403, 409, 410, 429 처럼 공통 응답이 없는 상태든,
 * 공통 응답과 겹치는 400, 401, 404 든 도메인 코드라면 적는다.
 * 적지 않으면 그 코드의 예시가 문서에서 통째로 빠지고, 호출하는 쪽은 공통 예시만 보게 된다.
 * 겹치는 상태에서는 {@link DomainErrorResponses} 가 공통 예시를 남긴 채 도메인 예시를 얹는다.
 *
 * <p>값은 enum 상수가 아니라 코드 문자열이다.
 * 애노테이션 배열은 한 가지 타입만 담을 수 있어서
 * 서로 다른 도메인의 enum 을 한 배열에 섞을 수 없기 때문이다.
 *
 * <pre>
 * &#64;ApiErrorCodes({"PROJECT403", "PROJECT_ADMIN403"})
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

	String[] value();

}
