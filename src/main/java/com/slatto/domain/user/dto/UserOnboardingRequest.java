package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingRequest {

    @NotNull(message = "약관 동의 여부는 필수입니다.")
    @AssertTrue(message = "약관에 동의해야 합니다.")
    private Boolean agreedTerms;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Pattern(
        regexp = "^(?=.*\\S)[가-힣a-zA-Z0-9 ]{2,20}$",
        message = "닉네임은 특수문자 없이 2자 이상 20자 이하로 입력해야 합니다."
    )
    private String nickname;

    @NotEmpty(message = "활동 역할은 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "활동 역할은 필수입니다.") RoleName> roles;

    @NotEmpty(message = "활동 지역은 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "활동 지역은 필수입니다.") RegionName> regions;

    @NotEmpty(message = "관심 카테고리는 1개 이상 선택해야 합니다.")
    private List<@NotNull(message = "관심 카테고리는 필수입니다.") CategoryName> categories;

    @Size(max = 200, message = "소개는 200자 이하로 입력해야 합니다.")
    private String bio;

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하로 입력해야 합니다.")
    private String profileImageUrl;
}
