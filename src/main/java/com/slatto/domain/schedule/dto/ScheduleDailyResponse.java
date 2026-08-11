package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "특정 날짜 일정 조회 응답")
public class ScheduleDailyResponse {

    @Schema(description = "조회 날짜", example = "2026-07-05")
    private LocalDate date;

    @Schema(description = "해당 날짜에 표시할 일정 목록")
    private List<DailySchedule> items;

    @Getter
    @Builder
    @Schema(description = "특정 날짜 일정 항목")
    public static class DailySchedule {

        @Schema(description = "일정 ID", example = "10")
        private Long scheduleId;

        @Schema(description = "일정 구분", example = "PROJECT")
        private ScheduleScope scheduleScope;

        @Schema(description = "프로젝트 ID. 개인 일정이면 null입니다.", example = "3", nullable = true)
        private Long projectId;

        @Schema(description = "프로젝트명. 개인 일정이면 null입니다.", example = "00 프로젝트", nullable = true)
        private String projectTitle;

        @Schema(description = "일정 제목", example = "레퍼런스 회의")
        private String title;

        @Schema(description = "시작 일시", example = "2026-07-05T14:00:00")
        private LocalDateTime startAt;

        @Schema(description = "종료 일시", example = "2026-07-05T15:00:00")
        private LocalDateTime endAt;

        @Schema(description = "장소", example = "빛나리 스튜디오", nullable = true)
        private String location;

        @Schema(description = "프로젝트 일정 대상자 목록. 개인 일정이면 빈 배열입니다.")
        private List<Participant> participants;

        @Schema(description = "일정 대상자 요약 문구", example = "그린 외 1명")
        private String participantSummary;

        @Schema(description = "공용 메모", example = "레퍼런스 무드보드 잡기, 1차 검토하기", nullable = true)
        private String publicMemo;

        @Schema(description = "로그인한 사용자에게만 보이는 개인 메모", example = "회의 전에 레퍼런스 링크 확인", nullable = true)
        private String privateMemo;

        @Schema(description = "로그인한 사용자의 수정 가능 여부", example = "true")
        private boolean canEdit;
    }

    @Getter
    @Builder
    @Schema(description = "일정 대상자 정보")
    public static class Participant {

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "닉네임", example = "그린")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
        private String profileImageUrl;
    }
}
