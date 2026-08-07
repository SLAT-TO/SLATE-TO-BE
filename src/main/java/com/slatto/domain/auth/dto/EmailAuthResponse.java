package com.slatto.domain.auth.dto;

public record EmailAuthResponse(
	Long userId,
	String accessToken,
	Boolean onboardingCompleted
) {
}
