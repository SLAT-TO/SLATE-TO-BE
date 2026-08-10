package com.slatto.domain.user.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {

    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ONBOARDING409", "이미 온보딩을 완료한 유저입니다."),
    PROFILE_IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "USER_PROFILE_IMAGE_EMPTY400", "업로드할 프로필 이미지가 비어 있습니다."),
    PROFILE_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "USER_PROFILE_IMAGE_INVALID_TYPE400", "지원하지 않는 프로필 이미지 형식입니다."),
    PROFILE_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "USER_PROFILE_IMAGE_SIZE400", "프로필 이미지는 최대 2MB까지 업로드할 수 있습니다."),
    WITHDRAW_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "USER_WITHDRAW_PASSWORD401", "비밀번호가 일치하지 않습니다."),
    INVALID_PORTFOLIO_PERIOD(HttpStatus.BAD_REQUEST, "PORTFOLIO_PERIOD400", "종료일은 시작일보다 이전일 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
