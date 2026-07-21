package com.slatto.domain.project.dto;

import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProjectInvitationAcceptResponse {

    private Long projectId;

    private Long memberId;

    private List<RoleName> roleNames;

    private LocalDateTime joinedAt;
}
