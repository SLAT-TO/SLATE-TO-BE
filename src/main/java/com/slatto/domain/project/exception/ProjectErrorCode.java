package com.slatto.domain.project.exception;

import com.slatto.global.response.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements BaseCode {

    INVALID_PROJECT_PERIOD(HttpStatus.BAD_REQUEST, "PROJECT400", "프로젝트 마감일은 시작일보다 이전일 수 없습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404", "프로젝트를 찾을 수 없습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER404", "프로젝트 멤버를 찾을 수 없습니다."),
    PROJECT_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_INVITATION404", "초대 링크를 찾을 수 없습니다."),
    PROJECT_INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "PROJECT_INVITATION_EXPIRED400", "만료된 초대 링크입니다."),
    PROJECT_INVITATION_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "PROJECT_INVITATION409", "이미 수락된 초대 링크입니다."),
    PROJECT_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_NOTICE404", "프로젝트 공지를 찾을 수 없습니다."),
    PROJECT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_FILE404", "프로젝트 파일을 찾을 수 없습니다."),
    PROJECT_FILE_EMPTY(HttpStatus.BAD_REQUEST, "PROJECT_FILE_EMPTY400", "업로드할 파일이 비어 있습니다."),
    PROJECT_FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "PROJECT_FILE_INVALID_TYPE400", "지원하지 않는 파일 형식입니다."),
    PROJECT_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "PROJECT_FILE_SIZE400", "프로젝트 파일은 최대 100MB까지 업로드할 수 있습니다."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROJECT403", "프로젝트 접근 권한이 없습니다."),
    PROJECT_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "PROJECT_ADMIN403", "프로젝트 관리자 권한이 필요합니다."),
    PROJECT_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROJECT_MEMBER409", "이미 프로젝트에 참여 중인 멤버입니다."),
    PROJECT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PROJECT409", "무료 계정은 최대 5개의 프로젝트를 생성할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public boolean isSuccess() {
        return false;
    }
}
