package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserProfileUpdateResponse {

    private Long id;

    private String nickname;

    private String profileImageUrl;

    private String bio;

    private RegionName location;

    private RoleName primaryRole;

    private List<RoleName> roles;

    private List<CategoryName> categories;

    private LocalDateTime updatedAt;
}
