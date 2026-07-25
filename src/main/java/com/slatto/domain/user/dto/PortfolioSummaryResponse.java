package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PortfolioSummaryResponse {

    private Long id;

    private String title;

    private CategoryName type;

    private String customTypeName;

    private List<RoleName> roles;

    private String thumbnailUrl;

    private LocalDateTime createdAt;
}
