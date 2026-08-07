-- 이메일 회원가입·비밀번호 재설정에 쓰는 인증번호 발송·확인 이력을 저장한다.

CREATE TABLE email_verification
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    purpose       VARCHAR(30)  NOT NULL,
    code_hash     VARCHAR(64)  NOT NULL,
    expires_at    DATETIME(6)  NOT NULL,
    verified_at   DATETIME(6)  NULL,
    consumed_at   DATETIME(6)  NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 최신 발송 건 조회와 쿨다운/시간당 한도 계산이 모두 (email, purpose) 로 걸린다.
CREATE INDEX idx_email_verification_email_purpose
    ON email_verification (email, purpose, id);
