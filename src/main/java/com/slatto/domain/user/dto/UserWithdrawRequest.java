package com.slatto.domain.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWithdrawRequest {

    @NotNull(message = "탈퇴 동의 여부는 필수입니다.")
    @AssertTrue(message = "탈퇴에 동의해야 합니다.")
    private Boolean agreed;

    // 비밀번호가 설정된 계정만 재인증한다. 소셜로만 가입한 계정은 확인할 비밀번호가 없어
    // 필수로 걸면 탈퇴 자체가 막힌다. 소셜 재인증 방식은 미확정이라 이번 범위에 넣지 않는다.
    private String password;
}
