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
		return base(cookieProperties.oauthStateName(), state)
			.maxAge(cookieProperties.oauthStateMaxAge())
			.build();
	}

	public ResponseCookie expiredOauthState() {
		return base(cookieProperties.oauthStateName(), "")
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
