package com.slatto.domain.project.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectCreateRequest {

    @NotBlank(message = "프로젝트명은 필수입니다.")
    @Size(max = 255, message = "프로젝트명은 최대 255자까지 입력할 수 있습니다.")
    private String title;

    @NotNull(message = "프로젝트 유형은 필수입니다.")
    private CategoryName type;

    @NotNull(message = "영상 길이는 필수입니다.")
    private LengthType lengthType;

    @NotBlank(message = "프로젝트 설명은 필수입니다.")
    private String description;

    @NotNull(message = "프로젝트 마감일은 필수입니다.")
    private LocalDate endDate;

    @Size(max = 255, message = "클라이언트명은 최대 255자까지 입력할 수 있습니다.")
    private String clientName;

    private Kind kind;

    @NotEmpty(message = "프로젝트에서의 역할은 1개 이상 선택해야 합니다.")
    private List<RoleName> roleNames;
}
