package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserPublicProfileResponse {

    private Long id;

    private String nickname;

    private String profileImageUrl;

    private String bio;

    private List<RegionName> locations;

    private RoleName primaryRole;

    private List<RoleName> roles;

    private List<CategoryName> categories;

    // 포트폴리오 기준 유형·역할 분포다. 같은 값을 GET /users/{userId}/stats 로도 단독 조회할 수 있다.
    private UserStatsResponse stats;
}
