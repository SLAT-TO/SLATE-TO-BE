package com.slatto.domain.notification.enums;

/**
 * 알림 클릭 시 프론트가 이동할 대상 종류.
 * DB에는 문자열로 저장하며, ERD의 notification.target_type 값과 대응된다.
 */
public enum NotificationTargetType {
    SCHEDULE,
    PROJECT,
    VIDEO,
    RECRUITMENT,
    NOTICE,
    PROJECT_FILE
}
