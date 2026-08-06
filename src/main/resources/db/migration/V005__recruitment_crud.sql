-- 구인구직 공고 CRUD를 위해 분류/조회수/삭제 컬럼을 추가하고, 모집 상태를 수동 마감 플래그로 대체한다.

ALTER TABLE recruitment
    ADD COLUMN category        VARCHAR(255) NULL,
    ADD COLUMN length_type     VARCHAR(255) NULL,
    ADD COLUMN view_count      INT          NOT NULL DEFAULT 0,
    ADD COLUMN closed_manually BIT(1)       NOT NULL DEFAULT b'0',
    ADD COLUMN deleted_at      DATETIME(6)  NULL;

UPDATE recruitment
SET closed_manually = b'1'
WHERE status = 'CLOSED';

ALTER TABLE recruitment
    MODIFY COLUMN status VARCHAR(255) NULL;

CREATE INDEX idx_recruitment_open
    ON recruitment (deleted_at, closed_manually, deadline);

ALTER TABLE recruitment_application
    ADD COLUMN reference_link VARCHAR(500) NULL;
