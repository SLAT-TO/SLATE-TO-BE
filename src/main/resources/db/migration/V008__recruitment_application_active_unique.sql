-- 중복 지원 경쟁 상태를 DB 레벨에서 막는다.
-- exists 사전 체크와 INSERT 사이에 동시 요청이 들어오면 두 요청 모두 검사를 통과해 지원 행이 2건 생긴다.
-- deleted_at 이 있는 행은 active_user_id 가 NULL 이 되고 MySQL 유니크 인덱스는 NULL 을 서로 다른 값으로
-- 취급하므로, 단순 (recruitment_id, user_id) 유니크와 달리 지원 취소 후 재지원이 차단되지 않는다.

-- 기존에 이미 중복된 활성 지원이 있는 경우, 가장 이른 행만 유지하고 나머지는 soft-delete 한다.
UPDATE recruitment_application ra
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY recruitment_id, user_id
                   ORDER BY id ASC
               ) AS rn
        FROM recruitment_application
        WHERE deleted_at IS NULL
    ) ranked
    WHERE rn > 1
) dup ON dup.id = ra.id
SET ra.deleted_at = CURRENT_TIMESTAMP;

ALTER TABLE recruitment_application
    ADD COLUMN active_user_id BIGINT
        GENERATED ALWAYS AS (IF(deleted_at IS NULL, user_id, NULL)) VIRTUAL;

ALTER TABLE recruitment_application
    ADD UNIQUE KEY uq_recruitment_application_active (recruitment_id, active_user_id);
