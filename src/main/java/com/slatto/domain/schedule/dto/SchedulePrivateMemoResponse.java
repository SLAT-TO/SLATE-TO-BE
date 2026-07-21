package com.slatto.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SchedulePrivateMemoResponse {

    private Long privateMemoId;

    private Long scheduleId;

    private String content;

    private LocalDateTime updatedAt;
}
