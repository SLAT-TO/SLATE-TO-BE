package com.slatto.domain.sharelink.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShareLinkErrorCode implements BaseCode {

    SHARE_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "SHARELINK404", "공유 링크를 찾을 수 없습니다."),
    SHARE_LINK_ALREADY_EXISTS(HttpStatus.CONFLICT, "SHARELINK409", "이미 이 영상의 공유 링크가 존재합니다."),
    INVALID_EXPIRED_AT(HttpStatus.BAD_REQUEST, "SHARELINK400", "만료 일시는 현재 시각보다 이후여야 합니다."),
    SHARE_LINK_UNAVAILABLE(HttpStatus.GONE, "SHARELINK410", "비활성화되었거나 만료된 링크입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}