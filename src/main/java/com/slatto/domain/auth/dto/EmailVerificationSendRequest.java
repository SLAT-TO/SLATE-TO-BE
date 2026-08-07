package com.slatto.domain.auth.dto;

import com.slatto.domain.auth.enums.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailVerificationSendRequest(

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 255, message = "이메일은 255자 이하로 입력해야 합니다.")
	String email,

	@NotNull(message = "인증 용도는 필수입니다.")
	VerificationPurpose purpose
) {
}
