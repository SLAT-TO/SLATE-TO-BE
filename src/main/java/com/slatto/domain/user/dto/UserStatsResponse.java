package com.slatto.domain.user.dto;

import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserStatsResponse {

    // 등록한 작업이 많은 유형부터 내려간다. 포트폴리오가 없으면 빈 배열이다.
    private List<ProjectTypeStat> projectTypes;

    private List<RoleStat> roles;

    public static UserStatsResponse empty() {
        return UserStatsResponse.builder()
            .projectTypes(List.of())
            .roles(List.of())
            .build();
    }

    @Getter
    @Builder
    public static class ProjectTypeStat {

        private CategoryName type;

        private Long count;
    }

    @Getter
    @Builder
    public static class RoleStat {

        private RoleName role;

        private Long count;
    }
}
