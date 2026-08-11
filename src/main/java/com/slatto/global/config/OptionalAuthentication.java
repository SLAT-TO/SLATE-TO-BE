package com.slatto.global.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 토큰이 있어도 되고 없어도 되는 엔드포인트임을 문서에 표시한다.
 *
 * <p>게스트 참여 경로는 인증 없이 열려 있지만, 토큰을 함께 보내면 로그인 사용자로 처리된다.
 * 문서에서 이 둘은 구분되지 않는다. 자물쇠만 보면 토큰이 필수처럼 읽히고,
 * 자물쇠를 떼면 로그인 사용자로 호출할 방법이 없는 것처럼 읽힌다.
 *
 * <p>이 표시가 붙으면 OpenAPI 의 security 에 빈 요구사항을 함께 넣어 둘 다 허용임을 나타내고,
 * 401 은 문서화하지 않는다. 실제로 인증 실패로 막히지 않는 경로이기 때문이다.
 *
 * <p>동작에는 영향을 주지 않는다. 실제 접근 제어는 {@code SecurityConfig} 가 결정한다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalAuthentication {
}
