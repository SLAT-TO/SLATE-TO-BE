-- 프로젝트 멤버별 최근활동 읽음 상태를 활동 로그 단위로 저장한다.
-- 이전 확인 시각 방식이 일부 DB에 반영된 경우를 함께 정리한다.

ALTER TABLE project_member
    DROP COLUMN IF EXISTS last_activity_read_at;

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

-- 인덱스는 V011에서 별도 생성
