package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
	String refreshTokenName,
	String oauthStateName,
	String path,
	boolean secure,
	String sameSite,
	String oauthStateSameSite,
	Duration oauthStateMaxAge
) {

	private static final String SAME_SITE_NONE = "none";
	private static final Set<String> ALLOWED_SAME_SITE = Set.of("lax", "strict", SAME_SITE_NONE);

	public CookieProperties {
		validateSameSite("app.cookie.same-site", sameSite, secure);
		validateSameSite("app.cookie.oauth-state-same-site", oauthStateSameSite, secure);
	}

	private static void validateSameSite(String property, String sameSite, boolean secure) {
		if (sameSite == null || !ALLOWED_SAME_SITE.contains(sameSite.toLowerCase(Locale.ROOT))) {
			throw new IllegalStateException(
				property + "는 Lax, Strict, None 중 하나여야 합니다. 현재 값: " + sameSite
			);
		}

		// Secure 없는 SameSite=None 쿠키는 브라우저가 저장·전송을 거부하므로 크로스 사이트 재발급이 조용히 실패한다.
		if (SAME_SITE_NONE.equalsIgnoreCase(sameSite) && !secure) {
			throw new IllegalStateException(
				property + "=None이면 app.cookie.secure=true여야 합니다. "
					+ "COOKIE_SAME_SITE=None으로 두려면 COOKIE_SECURE=false를 제거하세요."
			);
		}
	}

}
