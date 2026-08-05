package com.slatto.domain.recruitment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentBookmarkResponse {

    private Long recruitmentId;

    // 등록은 항상 true, 해제는 항상 false 다. 래퍼여야 Jackson 이 키 이름을 유지한다.
    private Boolean isBookmarked;
}
