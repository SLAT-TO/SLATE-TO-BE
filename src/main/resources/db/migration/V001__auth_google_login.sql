-- Google OAuth 로그인 도입에 필요한 사용자 온보딩 상태와 리프레시 토큰 테이블을 추가한다.

ALTER TABLE users
    ADD COLUMN onboarding_completed BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE refresh_token
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_token (token),
    KEY idx_refresh_token_user_id (user_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
