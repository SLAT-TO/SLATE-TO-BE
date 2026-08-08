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
}
