package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
	Duration codeValidity,
	Duration resendCooldown,
	Duration verifiedValidity,
	int maxSendPerHour,
	int maxAttempts
) {
}
