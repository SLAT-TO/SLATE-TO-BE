-- 그룹핑 알림의 누적 이벤트 개수를 관리한다.

ALTER TABLE notification
    ADD COLUMN group_count INT NOT NULL DEFAULT 1;
