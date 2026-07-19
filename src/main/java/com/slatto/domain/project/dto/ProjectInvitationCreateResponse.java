package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectInvitationCreateResponse {

    private String inviteUrl;

    private LocalDateTime expiresAt;
}
