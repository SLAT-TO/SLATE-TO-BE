package com.slatto.domain.feedback.support;

import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;

/**
 * 게스트 인증 헤더(X-Guest-Id / X-Guest-Token)는 짝으로만 유효하다.
 * 하나만 오면 잘못된 요청(400)으로 막는다.
 * 둘 다 없으면 회원 경로, 둘 다 있으면 게스트 경로로 통과시킨다.
 */
public final class GuestHeaderValidator {

    private GuestHeaderValidator() {
    }

    public static void validatePair(Long guestId, String guestToken) {
        boolean hasId = (guestId != null);
        boolean hasToken = (guestToken != null && !guestToken.isBlank());
        if (hasId != hasToken) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }
}