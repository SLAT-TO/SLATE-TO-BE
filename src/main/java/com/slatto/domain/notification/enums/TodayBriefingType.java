package com.slatto.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TodayBriefingType {

    TODAY_SCHEDULE(1),
    SCHEDULE_DUE_TODAY(2),
    SCHEDULE_START_REMINDER(3),
    RECRUITMENT_APPLIED(4),
    SCHEDULE_CREATED(5),
    NOTICE_CREATED(6),
    FILE_UPLOADED(7),
    VIDEO_FEEDBACK_COMMENTED(8);

    private final int priority;
}
