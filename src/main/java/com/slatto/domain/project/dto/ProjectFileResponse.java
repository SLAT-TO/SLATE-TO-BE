package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectFileResponse {

    private Long id;

    private String fileName;

    private String description;

    private String contentType;

    private Long fileSize;

    private Boolean isPinned;

    private Boolean isFinal;

    private UploaderSummary uploader;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class UploaderSummary {

        private Long id;

        private String nickname;
    }
}
