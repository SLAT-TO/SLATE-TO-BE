-- 프로젝트 공지의 사용자별 읽음 상태를 관리한다.

CREATE TABLE project_notice_read
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    notice_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    read_at    DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_notice_read_notice_user (notice_id, user_id),
    KEY idx_project_notice_read_user_id (user_id),
    KEY idx_project_notice_read_notice_id (notice_id),
    CONSTRAINT fk_project_notice_read_notice FOREIGN KEY (notice_id) REFERENCES project_notice (id),
    CONSTRAINT fk_project_notice_read_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
