-- 파일 업로드 시 항상 저장되는 실제 파일 크기와 스키마 제약을 일치시킨다.
-- 운영 DB 확인 결과 file_size IS NULL 행은 0건이므로 별도 backfill은 필요하지 않다.

ALTER TABLE project_file
    MODIFY COLUMN file_size BIGINT NOT NULL;
