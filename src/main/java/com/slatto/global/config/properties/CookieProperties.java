package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
	String refreshTokenName,
	String oauthStateName,
	String path,
	boolean secure,
	String sameSite,
	Duration oauthStateMaxAge
) {
}
