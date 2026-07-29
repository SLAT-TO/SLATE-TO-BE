package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PortfolioDetailResponse {

    private Long id;

    private String title;

    private CategoryName type;

    private String customTypeName;

    private Kind kind;

    private String clientName;

    private List<RoleName> roles;

    private String description;

    private String comment;

    private String youtubeUrl;

    private String thumbnailUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
