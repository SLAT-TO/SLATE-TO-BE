package com.slatto.domain.auth.support;

import java.security.SecureRandom;
import java.util.Base64;

public record OAuthState(String value, String redirectPath) {

	private static final String DELIMITER = "|";
	private static final SecureRandom RANDOM = new SecureRandom();

	public static OAuthState create(String redirectPath) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);

		return new OAuthState(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), redirectPath);
	}

	public String toCookieValue() {
		return value + DELIMITER + redirectPath;
	}

	public static OAuthState fromCookieValue(String cookieValue) {
		int index = cookieValue.indexOf(DELIMITER);

		if (index < 0) {
			return null;
		}

		return new OAuthState(cookieValue.substring(0, index), cookieValue.substring(index + 1));
	}

}
