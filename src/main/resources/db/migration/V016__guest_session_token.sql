-- 게스트 본인 확인용 세션 토큰 컬럼 추가
-- 기존 게스트 데이터에도 UUID를 채운 뒤 NOT NULL + UNIQUE 제약을 건다
ALTER TABLE guest ADD COLUMN session_token VARCHAR(36);
UPDATE guest SET session_token = UUID() WHERE session_token IS NULL;
ALTER TABLE guest MODIFY COLUMN session_token VARCHAR(36) NOT NULL UNIQUE;