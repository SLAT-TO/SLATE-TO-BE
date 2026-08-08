package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentCreateRequest {

    @NotBlank(message = "공고 제목은 필수입니다.")
    @Size(min = 5, max = 50, message = "공고 제목은 5자 이상 50자 이하로 입력해야 합니다.")
    private String title;

    private CategoryName category;

    private LengthType lengthType;

    @NotNull(message = "모집 파트는 필수입니다.")
    private RoleName recruitPart;

    private RegionName location;

    @Pattern(regexp = "(?s).*\\S.*", message = "촬영 기간은 공백일 수 없습니다.")
    @Size(max = 50, message = "촬영 기간은 최대 50자까지 입력할 수 있습니다.")
    private String shootingPeriod;

    @Pattern(regexp = "(?s).*\\S.*", message = "급여는 공백일 수 없습니다.")
    @Size(max = 50, message = "급여는 최대 50자까지 입력할 수 있습니다.")
    private String pay;

    // 지원자가 연락할 방법이 없으면 공고가 성립하지 않아 선택으로 열지 않는다.
    @NotBlank(message = "연락처는 필수입니다.")
    @Size(max = 100, message = "연락처는 최대 100자까지 입력할 수 있습니다.")
    private String contact;

    @NotBlank(message = "상세 내용은 필수입니다.")
    @Size(max = 2000, message = "상세 내용은 최대 2000자까지 입력할 수 있습니다.")
    private String description;

    // 마감일을 비우면 수동으로 마감할 때까지 모집이 유지된다.
    @FutureOrPresent(message = "마감일은 오늘 또는 그 이후여야 합니다.")
    private LocalDate deadline;
}
