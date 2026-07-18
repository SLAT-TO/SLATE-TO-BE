package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
	String secret,
	String issuer,
	Duration accessTokenValidity,
	Duration refreshTokenValidity
) {
}
