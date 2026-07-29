package com.slatto.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 수정 요청")
public class ScheduleUpdateRequest {

    @Schema(description = "일정 제목. 변경하지 않으면 생략하거나 null로 전달합니다.", example = "레퍼런스 회의 수정")
    @Size(max = 255, message = "일정 제목은 최대 255자까지 입력할 수 있습니다.")
    private String title;

    @Schema(description = "시작 일시. 변경하지 않으면 생략하거나 null로 전달합니다.", example = "2026-07-05T15:00:00")
    private LocalDateTime startAt;

    @Schema(description = "종료 일시. 변경하지 않으면 생략하거나 null로 전달합니다.", example = "2026-07-05T16:00:00")
    private LocalDateTime endAt;

    @Schema(description = "장소. 변경하지 않으면 생략하거나 null로 전달합니다.", example = "빛나리 스튜디오", nullable = true)
    @Size(max = 255, message = "장소는 최대 255자까지 입력할 수 있습니다.")
    private String location;

    @Schema(description = "공용 메모. 변경하지 않으면 생략하거나 null로 전달합니다.", example = "수정된 회의 메모", nullable = true)
    private String publicMemo;

    @Schema(description = "프로젝트 일정 대상자 사용자 ID 목록. 생략하면 기존 대상자를 유지하고, 빈 배열을 전달하면 모든 대상자를 제거합니다.", example = "[1, 3]")
    private List<Long> participantIds;
}
