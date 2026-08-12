-- notification.type 이 레거시 스키마의 ENUM 으로 남아 NotificationType 값 중 4개를 저장하지 못했다.
-- PROJECT_JOINED, SCHEDULE_CREATED, NOTICE_CREATED, FILE_UPLOADED 를 넣으면 strict 모드에서
-- Data truncated 로 끊겨 초대 수락·공지 등록·일정 등록·파일 업로드가 500 을 냈다.
-- V009 에서 activity_log.type 을 VARCHAR 로 바꾼 것과 같은 처리를 한다.

ALTER TABLE notification
    MODIFY COLUMN type VARCHAR(50) NOT NULL;

-- 레거시 ENUM 에만 있던 PROJECT_INVITED 는 Java enum 에 없어 조회 시 역직렬화가 깨진다.
-- 같은 뜻인 PROJECT_JOINED 로 정규화한다. ENUM 이던 동안에는 이 값을 쓸 수 없어 폭을 넓힌 뒤에 실행해야 한다.
UPDATE notification
SET type = 'PROJECT_JOINED'
WHERE type = 'PROJECT_INVITED';
