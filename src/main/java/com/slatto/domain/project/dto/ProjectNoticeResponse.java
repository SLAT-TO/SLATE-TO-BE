package com.slatto.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    private Boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 공고 DTO 에도 같은 이름의 중첩 클래스가 있다.
    // springdoc 은 단순 클래스명으로 스키마를 만들어 셋이 하나로 합쳐지므로 이름을 따로 준다.
    @Getter
    @Builder
    @Schema(name = "ProjectNoticeWriterSummary")
    public static class WriterSummary {

        private Long id;

        private String nickname;
    }
}
