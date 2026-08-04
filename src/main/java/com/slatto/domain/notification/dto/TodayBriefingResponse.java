package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.enums.TodayBriefingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "오늘의 브리핑 조회 응답")
public class TodayBriefingResponse {

    @Schema(description = "오늘의 브리핑 목록")
    private List<BriefingItem> items;

    @Getter
    @Builder
    @Schema(description = "오늘의 브리핑 항목")
    public static class BriefingItem {

        @Schema(description = "브리핑 유형", example = "TODAY_SCHEDULE")
        private TodayBriefingType type;

        @Schema(description = "브리핑 표시 문구", example = "[슬레이트] 오늘 [레퍼런스 회의] 일정이 있어요")
        private String content;

        @Schema(description = "브리핑 우선순위", example = "1")
        private Integer priority;

        @Schema(description = "관련 프로젝트 ID", example = "3")
        private Long projectId;

        @Schema(description = "관련 대상 종류", example = "SCHEDULE")
        private String targetType;

        @Schema(description = "관련 대상 ID", example = "10")
        private Long targetId;

        @Schema(description = "브리핑 기준 일시", example = "2026-08-04T09:00:00")
        private LocalDateTime occurredAt;
    }
}
