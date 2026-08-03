package com.slatto.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TodayBriefingType {

    TODAY_SCHEDULE(1),
    SCHEDULE_DEADLINE(2),
    RECRUITMENT_APPLIED(3),
    SCHEDULE_CREATED(4),
    NOTICE_CREATED(5),
    FILE_UPLOADED(6),
    VIDEO_FEEDBACK_COMMENTED(7);

    private final int priority;
}
