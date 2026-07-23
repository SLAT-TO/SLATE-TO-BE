package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.entity.NotificationSetting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingResponse {

    private Boolean emailAllEnabled;

    private Boolean emailDeadlineReminder;

    private Boolean emailAssigned;

    private Boolean emailNewApplicant;

    private Boolean emailMissedSummary;

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
            .emailAllEnabled(setting.getEmailAllEnabled())
            .emailDeadlineReminder(setting.getEmailDeadlineReminder())
            .emailAssigned(setting.getEmailAssigned())
            .emailNewApplicant(setting.getEmailNewApplicant())
            .emailMissedSummary(setting.getEmailMissedSummary())
            .build();
    }
}
