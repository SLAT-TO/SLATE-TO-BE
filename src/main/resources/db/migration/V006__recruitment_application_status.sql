-- 지원 상태 탭 필터가 레거시 NULL 행을 누락시키지 않도록 기본값을 보장한다.

UPDATE recruitment_application
SET status = 'PENDING'
WHERE status IS NULL
   OR status NOT IN ('PENDING', 'ACCEPTED', 'REJECTED');

ALTER TABLE recruitment_application
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';
