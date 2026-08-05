package com.slatto.domain.recruitment.dto;

import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PENDING 은 유효한 enum 값이라 Bean Validation 으로 못 막는다. 서비스에서 400 으로 거른다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplicationStatusUpdateRequest {

    @NotNull(message = "변경할 지원 상태는 필수입니다.")
    private RecruitmentApplicationStatus status;
}
