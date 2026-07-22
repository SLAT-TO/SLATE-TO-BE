package com.slatto.domain.project.dto;

import com.slatto.domain.project.enums.ExpirationPeriod;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectInvitationCreateRequest {

    private ExpirationPeriod expirationPeriod;
}
