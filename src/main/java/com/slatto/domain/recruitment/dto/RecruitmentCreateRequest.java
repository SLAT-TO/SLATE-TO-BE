package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentCreateRequest {

    @NotBlank(message = "공고 제목은 필수입니다.")
    @Size(max = 100, message = "공고 제목은 최대 100자까지 입력할 수 있습니다.")
    private String title;

    @NotNull(message = "카테고리는 필수입니다.")
    private CategoryName category;

    @NotNull(message = "영상 길이 유형은 필수입니다.")
    private LengthType lengthType;

    @NotNull(message = "모집 파트는 필수입니다.")
    private RoleName recruitPart;

    @NotNull(message = "지역은 필수입니다.")
    private RegionName location;

    @NotBlank(message = "촬영 기간은 필수입니다.")
    @Size(max = 50, message = "촬영 기간은 최대 50자까지 입력할 수 있습니다.")
    private String shootingPeriod;

    @NotBlank(message = "급여는 필수입니다.")
    @Size(max = 50, message = "급여는 최대 50자까지 입력할 수 있습니다.")
    private String pay;

    @NotBlank(message = "연락처는 필수입니다.")
    @Size(max = 100, message = "연락처는 최대 100자까지 입력할 수 있습니다.")
    private String contact;

    @NotBlank(message = "상세 내용은 필수입니다.")
    @Size(max = 2000, message = "상세 내용은 최대 2000자까지 입력할 수 있습니다.")
    private String description;

    @NotNull(message = "마감일은 필수입니다.")
    @FutureOrPresent(message = "마감일은 오늘 또는 그 이후여야 합니다.")
    private LocalDate deadline;
}
