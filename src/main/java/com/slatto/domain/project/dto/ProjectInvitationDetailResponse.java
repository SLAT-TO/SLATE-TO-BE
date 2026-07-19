package com.slatto.domain.project.dto;

import com.slatto.domain.project.enums.InvitationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectInvitationDetailResponse {

    private Long projectId;

    private String projectTitle;

    private String inviterName;

    private InvitationStatus status;

    private LocalDateTime expiresAt;
}
