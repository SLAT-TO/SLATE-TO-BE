package com.slatto.domain.project.dto;

import com.slatto.domain.project.enums.Permission;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProjectMemberListResponse {

    private List<MemberSummary> items;

    private Long memberCount;

    @Getter
    @Builder
    public static class MemberSummary {

        private Long memberId;

        private Long userId;

        private String nickname;

        private String profileImageUrl;

        private Permission permission;

        private List<RoleName> roleNames;

        private LocalDateTime joinedAt;
    }
}
