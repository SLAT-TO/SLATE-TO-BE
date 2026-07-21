package com.slatto.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulePrivateMemoRequest {

    @NotBlank(message = "개인 메모 내용은 필수입니다.")
    private String content;
}
