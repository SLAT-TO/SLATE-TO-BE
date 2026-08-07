package com.slatto.domain.auth.dto;

import com.slatto.domain.auth.enums.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@NotBlank(message = "인증번호는 필수입니다.")
	@Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
	String code,

	@NotNull(message = "인증 용도는 필수입니다.")
	VerificationPurpose purpose
) {
}
