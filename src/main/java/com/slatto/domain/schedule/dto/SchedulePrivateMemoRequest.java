package com.slatto.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 개인 메모 저장/수정 요청")
public class SchedulePrivateMemoRequest {

    @Schema(description = "나에게만 보이는 개인 메모 내용", example = "회의 전에 레퍼런스 링크 확인")
    @NotBlank(message = "개인 메모 내용은 필수입니다.")
    private String content;
}
