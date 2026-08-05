package com.slatto.domain.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplicationCreateRequest {

    @NotBlank(message = "지원 메시지는 필수입니다.")
    @Size(max = 1000, message = "지원 메시지는 1000자 이하여야 합니다.")
    private String message;

    @Size(max = 500, message = "참고 링크는 500자 이하여야 합니다.")
    private String referenceLink;
}
