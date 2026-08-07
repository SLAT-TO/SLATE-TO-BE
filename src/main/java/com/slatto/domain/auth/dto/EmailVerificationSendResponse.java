package com.slatto.domain.auth.dto;

import java.time.LocalDateTime;

public record EmailVerificationSendResponse(
	LocalDateTime expiresAt,
	LocalDateTime resendAvailableAt
) {
}
