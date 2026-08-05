-- 구인구직처럼 프로젝트와 무관한 알림을 저장할 수 있도록 project_id 를 nullable 로 완화한다.
-- Notification 엔티티는 이미 nullable = true 로 선언돼 있으나 ddl-auto=validate 는 nullable 을 검증하지 않아
-- 실제 컬럼이 NOT NULL 인 채로 남아 있었다.

ALTER TABLE notification
    MODIFY COLUMN project_id BIGINT NULL;
