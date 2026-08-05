-- 과거 activity_log.type 컬럼이 제작 역할(RoleName) enum으로 생성된 문제를 바로잡는다.
-- 이후 ActivityLogType enum 값(PROJECT_UPDATED, VIDEO_FEEDBACK_COMMENTED 등)을 문자열로 저장한다.

ALTER TABLE activity_log
    MODIFY COLUMN type VARCHAR(50) NOT NULL;
