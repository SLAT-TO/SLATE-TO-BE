package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectInvitationAcceptResponse {

    private Long projectId;

    private Long memberId;

    private LocalDateTime joinedAt;
}
