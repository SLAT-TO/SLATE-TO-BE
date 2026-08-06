-- 프로젝트 멤버별 최근활동 읽음 상태를 활동 로그 단위로 저장한다.
-- MySQL 은 DROP COLUMN IF EXISTS / CREATE INDEX IF NOT EXISTS 같은 조건부 DDL 을
-- 지원하지 않으므로(MariaDB 전용 확장) 조건부 없이 작성한다.

CREATE TABLE project_activity_read (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_member_id BIGINT NOT NULL,
    activity_log_id BIGINT NOT NULL,
    read_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_activity_read_member_log UNIQUE (project_member_id, activity_log_id),
    CONSTRAINT fk_project_activity_read_member
        FOREIGN KEY (project_member_id) REFERENCES project_member (id),
    CONSTRAINT fk_project_activity_read_activity_log
        FOREIGN KEY (activity_log_id) REFERENCES activity_log (id)
);

-- activity_log (project_id, created_at, id) 인덱스는 운영 DB에 이미 존재하며
-- FK(project_id -> project.id) 를 떠받치고 있어 제거할 수 없으므로 마이그레이션에 포함하지 않는다.
