package com.slatto.domain.project.dto;

import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMemberUpdateRequest {

    @NotEmpty(message = "프로젝트에서의 역할은 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "프로젝트에서의 역할은 필수입니다.") RoleName> roleNames;
}
