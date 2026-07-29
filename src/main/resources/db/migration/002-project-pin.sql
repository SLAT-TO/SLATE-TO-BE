-- 프로젝트 목록/상세에서 사용자별 프로젝트 상단 고정 상태를 관리한다.

CREATE TABLE project_pin
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    project_id BIGINT      NOT NULL,
    pinned_at  DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_pin_user_project (user_id, project_id),
    KEY idx_project_pin_user_id (user_id),
    KEY idx_project_pin_project_id (project_id),
    CONSTRAINT fk_project_pin_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_project_pin_project FOREIGN KEY (project_id) REFERENCES project (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
