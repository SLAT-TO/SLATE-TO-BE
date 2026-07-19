package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserMeResponse {

    private Long id;

    private String email;

    private String nickname;

    private String profileImageUrl;

    private String bio;

    private RegionName region;

    private SocialType socialType;

    private RoleName primaryRole;

    private List<RoleName> roles;

    private List<CategoryName> categories;

    private Boolean onboardingCompleted;

    private LocalDateTime createdAt;
}
