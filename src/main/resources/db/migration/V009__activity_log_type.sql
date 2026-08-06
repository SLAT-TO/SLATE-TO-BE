-- 과거 activity_log.type 컬럼이 제작 역할(RoleName) enum으로 생성된 문제를 바로잡는다.
-- 이후 ActivityLogType enum 값(PROJECT_UPDATED, VIDEO_FEEDBACK_COMMENTED 등)을 문자열로 저장한다.

-- 기존 레거시 값이 있으면 호환 가능한 문자열로 정규화한다.
UPDATE activity_log
SET type = CASE
    WHEN type IN (
        'PROJECT_MEMBER_JOINED',
        'PROJECT_STATUS_CHANGED',
        'PROJECT_UPDATED',
        'SCHEDULE_CREATED',
        'SCHEDULE_UPDATED',
        'NOTICE_CREATED',
        'FILE_UPLOADED',
        'VIDEO_FEEDBACK_COMMENTED'
    ) THEN type
    WHEN type IN ('DIRECTOR', 'PD', 'CINEMATOGRAPHER', 'EDITOR', 'ART', 'SOUND', 'WRITER', 'LIGHTING', 'ACTOR', 'ETC') THEN 'PROJECT_UPDATED'
    ELSE 'PROJECT_UPDATED'
END
WHERE type IS NOT NULL;

ALTER TABLE activity_log
    MODIFY COLUMN type VARCHAR(50) NOT NULL;
