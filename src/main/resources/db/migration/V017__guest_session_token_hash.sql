-- session_token: UUID(36자) → SHA-256 해시(64자 hex) 저장용으로 확장
ALTER TABLE guest
    MODIFY COLUMN session_token VARCHAR(64) NOT NULL;