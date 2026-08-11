package com.slatto.domain.video.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VideoErrorCode implements BaseCode {

    VIDEO_ALREADY_REGISTERED(HttpStatus.CONFLICT, "VIDEO409", "이미 등록된 영상입니다."),
    VIDEO_REFERENCE_FILE_ALREADY_LINKED(
        HttpStatus.CONFLICT,
        "VIDEO_REFERENCE_FILE409",
        "이미 연결된 참고 자료입니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
