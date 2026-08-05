package com.slatto.domain.notification.dto;

import java.time.LocalDateTime;

public record ActivityLogCursor(
    LocalDateTime createdAt,
    Long activityId
) {

    public String toValue() {
        return createdAt + "_" + activityId;
    }
}
