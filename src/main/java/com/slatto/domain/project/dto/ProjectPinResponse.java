package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectPinResponse {

    private Long id;

    private Boolean isPinned;

    private LocalDateTime pinnedAt;
}
