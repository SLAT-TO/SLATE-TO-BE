-- 알림 제목과 그룹핑 알림의 누적 이벤트 개수를 관리한다.

ALTER TABLE notification
    ADD COLUMN title VARCHAR(255) NULL,
    ADD COLUMN group_count INT NOT NULL DEFAULT 1;

CREATE INDEX idx_notification_group_lookup
    ON notification (user_id, type, target_type, target_id, is_read);
