package com.slatto.domain.feedback.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackErrorCode implements BaseCode {

    FEEDBACK_WRITER_ONLY(HttpStatus.FORBIDDEN, "FEEDBACK403", "본인이 작성한 피드백만 수정하거나 삭제할 수 있습니다."),
    FEEDBACK_REPLY_WRITER_ONLY(HttpStatus.FORBIDDEN, "FEEDBACK_REPLY403", "본인이 작성한 답글만 수정하거나 삭제할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
