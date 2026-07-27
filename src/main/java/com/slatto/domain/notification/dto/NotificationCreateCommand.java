package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationCreateCommand {

    private List<Long> recipientIds;
    private Long projectId;
    private NotificationType type;
    private String content;
    private String targetType;
    private Long targetId;
    private Long excludeUserId;
}
