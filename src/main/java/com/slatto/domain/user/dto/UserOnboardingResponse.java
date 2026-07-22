package com.slatto.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserOnboardingResponse {

    private Long id;

    private Boolean onboardingCompleted;

    private LocalDateTime updatedAt;
}
