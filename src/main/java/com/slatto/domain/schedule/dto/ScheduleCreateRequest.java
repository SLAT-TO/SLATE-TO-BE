package com.slatto.domain.schedule.dto;

import com.slatto.domain.schedule.enums.ScheduleScope;
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
public class ScheduleCreateRequest {

    @NotNull(message = "일정 구분은 필수입니다.")
    private ScheduleScope scheduleScope;

    private Long projectId;

    @NotBlank(message = "일정 제목은 필수입니다.")
    @Size(max = 255, message = "일정 제목은 최대 255자까지 입력할 수 있습니다.")
    private String title;

    @NotNull(message = "일정 시작 일시는 필수입니다.")
    private LocalDateTime startAt;

    @NotNull(message = "일정 종료 일시는 필수입니다.")
    private LocalDateTime endAt;

    @Size(max = 255, message = "장소는 최대 255자까지 입력할 수 있습니다.")
    private String location;

    private String publicMemo;

    private List<Long> participantIds;
}
