package com.slatto.domain.notification.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettingUpdateRequest {

    private Boolean emailAllEnabled;

    private Boolean emailDeadlineReminder;

    private Boolean emailAssigned;

    private Boolean emailNewApplicant;

    private Boolean emailMissedSummary;
}
