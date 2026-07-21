package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectNoticeResponse {

    private Long id;

    private String title;

    private String content;

    private WriterSummary writer;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class WriterSummary {

        private Long id;

        private String nickname;
    }
}
