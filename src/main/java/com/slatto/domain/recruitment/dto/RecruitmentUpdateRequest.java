package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 부분 수정이므로 전 필드가 선택이다. @Size 는 null 을 통과시켜 "미전달 = 스킵"을 유지하면서 빈 문자열만 막는다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentUpdateRequest {

    @Size(min = 1, max = 100, message = "공고 제목은 1자 이상 100자 이하여야 합니다.")
    private String title;

    private CategoryName category;

    private LengthType lengthType;

    private RoleName recruitPart;

    private RegionName location;

    @Size(min = 1, max = 50, message = "촬영 기간은 1자 이상 50자 이하여야 합니다.")
    private String shootingPeriod;

    @Size(min = 1, max = 50, message = "급여는 1자 이상 50자 이하여야 합니다.")
    private String pay;

    @Size(min = 1, max = 100, message = "연락처는 1자 이상 100자 이하여야 합니다.")
    private String contact;

    @Size(min = 1, max = 2000, message = "상세 내용은 1자 이상 2000자 이하여야 합니다.")
    private String description;

    // 마감일 앞당기기가 정상 시나리오라 날짜 검증을 걸지 않는다.
    private LocalDate deadline;

    private RecruitmentStatus status;
}
