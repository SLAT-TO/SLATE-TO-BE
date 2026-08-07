package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.enums.ActivityLogType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ActivityLogListResponse(
    List<ActivityLogItem> items,
    String nextCursor,
    boolean hasNext
) {

    @Builder
    public record ActivityLogItem(
        Long activityId,
        ActivityLogType type,
        String content,
        String targetType,
        Long targetId,
        LocalDateTime createdAt,
        boolean isRead
    ) {
    }
}
