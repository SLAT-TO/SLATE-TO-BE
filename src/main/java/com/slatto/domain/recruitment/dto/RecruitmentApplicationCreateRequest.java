package com.slatto.domain.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplicationCreateRequest {

    @NotBlank(message = "지원 메시지는 필수입니다.")
    @Size(max = 1000, message = "지원 메시지는 1000자 이하여야 합니다.")
    private String message;

    @Size(max = 500, message = "참고 링크는 500자 이하여야 합니다.")
    private String referenceLink;

    // 첨부 파일 업로드 API 가 반환한 id 목록이다. 파일 본문이 아니라 id 만 받는다.
    @Size(max = 10, message = "첨부 파일은 최대 10개까지 등록할 수 있습니다.")
    private List<Long> fileIds;
}
