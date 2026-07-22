package com.slatto.domain.user.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {

    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ONBOARDING409", "이미 온보딩을 완료한 유저입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
