package com.slatto.domain.auth.dto;

import java.time.LocalDateTime;

public record EmailVerificationConfirmResponse(LocalDateTime verifiedUntil) {
}
