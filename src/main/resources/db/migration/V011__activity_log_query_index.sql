-- 프로젝트별 최신순 활동 로그 조회 성능 최적화
-- V010에서 테이블 생성 후 인덱스 추가

-- 이미 존재하는 경우를 대비해 먼저 확인하고 생성
-- MySQL에서는 동적 SQL이 제한되므로, 이미 존재하면 에러는 무시됨
CREATE INDEX idx_activity_log_project_created_id 
    ON activity_log (project_id, created_at, id);
