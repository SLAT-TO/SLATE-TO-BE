package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 부분 수정이므로 전 필드가 선택이다. @Pattern 과 @Size 는 null 을 통과시켜 "미전달 = 스킵"을 유지하면서
// 빈 문자열과 공백 전용 문자열만 막는다. (?s) 는 개행이 포함된 상세 내용을 위한 DOTALL 플래그다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentUpdateRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "공고 제목은 공백일 수 없습니다.")
    @Size(min = 5, max = 50, message = "공고 제목은 5자 이상 50자 이하여야 합니다.")
    private String title;

    private CategoryName category;

    private LengthType lengthType;

    private RoleName recruitPart;

    private RegionName location;

    @Pattern(regexp = "(?s).*\\S.*", message = "촬영 기간은 공백일 수 없습니다.")
    @Size(max = 50, message = "촬영 기간은 50자 이하여야 합니다.")
    private String shootingPeriod;

    @Pattern(regexp = "(?s).*\\S.*", message = "급여는 공백일 수 없습니다.")
    @Size(max = 50, message = "급여는 50자 이하여야 합니다.")
    private String pay;

    @Pattern(regexp = "(?s).*\\S.*", message = "연락처는 공백일 수 없습니다.")
    @Size(max = 100, message = "연락처는 100자 이하여야 합니다.")
    private String contact;

    @Pattern(regexp = "(?s).*\\S.*", message = "상세 내용은 공백일 수 없습니다.")
    @Size(max = 2000, message = "상세 내용은 2000자 이하여야 합니다.")
    private String description;

    // 마감일 앞당기기가 정상 시나리오라 날짜 검증을 걸지 않는다.
    private LocalDate deadline;

    private RecruitmentStatus status;
}
