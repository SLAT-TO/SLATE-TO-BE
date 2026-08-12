package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 생성 요청")
public class ScheduleCreateRequest {

    @Schema(description = "일정 구분. 개인 일정은 PERSONAL, 프로젝트 일정은 PROJECT입니다.", example = "PROJECT")
    @NotNull(message = "일정 구분은 필수입니다.")
    private ScheduleScope scheduleScope;

    @Schema(description = "프로젝트 ID. PROJECT 일정이면 필수, PERSONAL 일정이면 전달하지 않습니다.", example = "3", nullable = true)
    private Long projectId;

    @Schema(description = "일정 제목", example = "레퍼런스 회의")
    @NotBlank(message = "일정 제목은 필수입니다.")
    @Size(max = 255, message = "일정 제목은 최대 255자까지 입력할 수 있습니다.")
    private String title;

    @Schema(description = "시작 일시", example = "2026-07-05T14:00:00")
    @NotNull(message = "일정 시작 일시는 필수입니다.")
    private LocalDateTime startAt;

    @Schema(description = "종료 일시", example = "2026-07-05T15:00:00")
    @NotNull(message = "일정 종료 일시는 필수입니다.")
    private LocalDateTime endAt;

    @Schema(description = "장소", example = "빛나리 스튜디오", nullable = true)
    @Size(max = 255, message = "장소는 최대 255자까지 입력할 수 있습니다.")
    private String location;

    @Schema(description = "공용 메모", example = "레퍼런스 무드보드 잡기, 1차 검토하기", nullable = true)
    private String publicMemo;

    @Schema(description = "프로젝트 일정 대상자 사용자 ID 목록. PROJECT 일정에서 선택 항목이며, PERSONAL 일정이면 전달하지 않습니다.", example = "[1, 2]")
    private List<Long> participantIds;
}
