package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NotificationListResponse {

    private List<NotificationSummary> items;

    private Long nextCursor;

    private Boolean hasNext;

    @Getter
    @Builder
    public static class NotificationSummary {

        private Long notificationId;

        private Long projectId;

        private NotificationType type;

        private String content;

        private String targetType;

        private Long targetId;

        private Boolean isRead;

        private LocalDateTime readAt;

        private LocalDateTime createdAt;
    }
}
