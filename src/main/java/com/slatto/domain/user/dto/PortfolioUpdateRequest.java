package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioUpdateRequest {

    @Size(min = 1, max = 100, message = "프로젝트 제목은 1자 이상 100자 이하로 입력해야 합니다.")
    private String title;

    private CategoryName type;

    @Size(max = 100, message = "기타 유형명은 100자 이하로 입력해야 합니다.")
    private String customTypeName;

    private Kind kind;

    @Size(max = 255, message = "클라이언트명은 255자 이하로 입력해야 합니다.")
    private String clientName;

    @Size(min = 1, message = "맡은 역할은 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "맡은 역할은 필수입니다.") RoleName> roles;

    private String description;

    private String comment;

    @Size(max = 500, message = "영상 링크는 500자 이하로 입력해야 합니다.")
    private String youtubeUrl;
}
