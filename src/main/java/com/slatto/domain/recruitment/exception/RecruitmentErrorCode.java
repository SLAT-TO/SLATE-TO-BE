package com.slatto.domain.recruitment.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements BaseCode {

    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "RECRUITMENT_CLOSED400", "마감된 공고에는 지원할 수 없습니다."),
    RECRUITMENT_SELF_APPLICATION(HttpStatus.BAD_REQUEST, "RECRUITMENT_SELF400", "본인이 작성한 공고에는 지원할 수 없습니다."),
    APPLICATION_ALREADY_APPLIED(HttpStatus.CONFLICT, "APPLICATION409", "이미 지원한 공고입니다."),
    APPLICATION_ALREADY_HANDLED(HttpStatus.BAD_REQUEST, "APPLICATION_ALREADY_HANDLED400", "이미 수락 또는 거절 처리된 지원입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
