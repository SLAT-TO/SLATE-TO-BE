-- 프로젝트 멤버별 최근활동 확인 시각과 최근활동 목록 조회 인덱스를 추가한다.

ALTER TABLE project_member
    ADD COLUMN last_activity_read_at DATETIME(6) NULL;

CREATE INDEX idx_activity_log_project_created_id
    ON activity_log (project_id, created_at, id);
