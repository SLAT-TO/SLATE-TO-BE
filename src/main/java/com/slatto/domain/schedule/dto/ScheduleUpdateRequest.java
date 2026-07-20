package com.slatto.domain.schedule.dto;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleUpdateRequest {

    @Size(max = 255, message = "일정 제목은 최대 255자까지 입력할 수 있습니다.")
    private String title;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Size(max = 255, message = "장소는 최대 255자까지 입력할 수 있습니다.")
    private String location;

    private String publicMemo;

    private List<Long> participantIds;
}
