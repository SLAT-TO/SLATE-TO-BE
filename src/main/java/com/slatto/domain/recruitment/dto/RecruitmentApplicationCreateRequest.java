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
    // 개수 검증은 @Size 로 하지 않는다. 중복 id 를 제거한 뒤 세야 하고, 여기서 걸리면
    // 프론트가 분기할 수 없는 COMMON400 이 나가기 때문이다. 서비스가 도메인 코드로 처리한다.
    private List<Long> fileIds;
}
