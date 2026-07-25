package com.slatto.domain.user.dto;

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

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioCreateRequest {

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    @Size(min = 1, max = 100, message = "프로젝트 제목은 1자 이상 100자 이하로 입력해야 합니다.")
    private String title;

    @NotNull(message = "프로젝트 유형은 필수입니다.")
    private CategoryName type;

    @Size(max = 100, message = "기타 유형명은 100자 이하로 입력해야 합니다.")
    private String customTypeName;

    @NotNull(message = "개인/외주 구분은 필수입니다.")
    private Kind kind;

    @Size(max = 255, message = "클라이언트명은 255자 이하로 입력해야 합니다.")
    private String clientName;

    @NotEmpty(message = "맡은 역할은 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "맡은 역할은 필수입니다.") RoleName> roles;

    @NotBlank(message = "프로젝트 설명은 필수입니다.")
    private String description;

    private String comment;

    @NotBlank(message = "영상 링크는 필수입니다.")
    @Size(max = 500, message = "영상 링크는 500자 이하로 입력해야 합니다.")
    private String youtubeUrl;
}
