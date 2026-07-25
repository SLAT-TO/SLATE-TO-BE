package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectNoticeReadResponse {

    private Long id;

    private Boolean isRead;

    private LocalDateTime readAt;
}
