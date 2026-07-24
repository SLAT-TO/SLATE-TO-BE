package com.slatto.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PortfolioUpdateResponse {

    private Long id;

    private LocalDateTime updatedAt;
}
