package com.slatto.domain.auth.support;

import com.slatto.global.config.properties.CookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

	private final CookieProperties cookieProperties;

	public ResponseCookie refreshToken(String token, long maxAgeSeconds) {
		return base(cookieProperties.refreshTokenName(), token)
			.maxAge(maxAgeSeconds)
			.build();
	}

	public ResponseCookie expiredRefreshToken() {
		return base(cookieProperties.refreshTokenName(), "")
			.maxAge(0)
			.build();
	}

	public ResponseCookie oauthState(String state) {
		return oauthStateBase(state)
			.maxAge(cookieProperties.oauthStateMaxAge())
			.build();
	}

	public ResponseCookie expiredOauthState() {
		return oauthStateBase("")
			.maxAge(0)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String name, String value) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(cookieProperties.secure())
			.path(cookieProperties.path())
			.sameSite(cookieProperties.sameSite());
	}

	// 구글에서 콜백으로 돌아오는 요청은 크로스 사이트라 SameSite=Strict면 쿠키가 실리지 않는다.
	private ResponseCookie.ResponseCookieBuilder oauthStateBase(String value) {
		return base(cookieProperties.oauthStateName(), value)
			.sameSite(cookieProperties.oauthStateSameSite());
	}

	public String refreshTokenName() {
		return cookieProperties.refreshTokenName();
	}

	public String oauthStateName() {
		return cookieProperties.oauthStateName();
	}

	public Duration oauthStateMaxAge() {
		return cookieProperties.oauthStateMaxAge();
	}

}
