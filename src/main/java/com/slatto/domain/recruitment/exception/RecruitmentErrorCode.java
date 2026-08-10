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
    APPLICATION_ALREADY_HANDLED(HttpStatus.BAD_REQUEST, "APPLICATION_ALREADY_HANDLED400", "이미 수락 또는 거절 처리된 지원입니다."),
    RECRUITMENT_CLOSED_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "RECRUITMENT_CLOSED_EDIT400", "마감된 공고는 수정할 수 없습니다."),

    // 첨부 파일은 실패 사유마다 사용자에게 다른 안내를 띄워야 해서 코드를 나눈다.
    APPLICATION_FILE_EMPTY(HttpStatus.BAD_REQUEST, "APPLICATION_FILE_EMPTY400", "첨부 파일이 비어 있습니다."),
    APPLICATION_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "APPLICATION_FILE_SIZE400", "첨부 파일은 개당 100MB 이하여야 합니다."),
    APPLICATION_FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "APPLICATION_FILE_TYPE400", "지원하지 않는 파일 형식입니다. pdf, jpg, jpeg, png, webp, zip, mp4 만 첨부할 수 있습니다."),
    APPLICATION_FILE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "APPLICATION_FILE_LIMIT400", "첨부 파일은 최대 10개까지 등록할 수 있습니다."),
    APPLICATION_FILE_NOT_LINKABLE(HttpStatus.BAD_REQUEST, "APPLICATION_FILE_LINK400", "첨부할 수 없는 파일입니다. 파일을 다시 업로드해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
