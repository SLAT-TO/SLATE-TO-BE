package com.slatto.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PortfolioCreateResponse {

    private Long id;

    private String thumbnailUrl;

    private LocalDateTime createdAt;
}
