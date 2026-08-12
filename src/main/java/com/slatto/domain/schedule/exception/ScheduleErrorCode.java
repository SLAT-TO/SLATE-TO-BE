package com.slatto.domain.schedule.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseCode {

    SCHEDULE_WRITER_ONLY(HttpStatus.FORBIDDEN, "SCHEDULE403", "본인이 등록한 일정만 조회하거나 변경할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
