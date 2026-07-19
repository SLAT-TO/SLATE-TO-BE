-- ddl-auto=validate 이므로 애플리케이션 기동 전에 직접 적용해야 한다.

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
