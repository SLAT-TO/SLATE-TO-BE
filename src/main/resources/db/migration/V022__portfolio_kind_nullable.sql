-- 포트폴리오의 개인/외주 구분을 선택 입력으로 완화
--
-- 배경:
--   이력 등록 시 개인 작업인지 외주 작업인지 반드시 고르게 되어 있었으나,
--   목록 조회나 필터에 쓰이지 않아 등록 단계에서 불필요한 선택을 강요하는 상태였다.
--   엔티티에서 nullable 로 바꾸면서 컬럼 제약도 함께 푼다.
--
-- ENUM -> VARCHAR:
--   운영 DB 는 ddl-auto=update 로 생성된 구간이 있어 이 컬럼이 MySQL ENUM 일 수 있다.
--   ENUM 은 값을 추가할 때마다 ALTER TABLE 이 필요하고, Java enum 과 어긋나면
--   strict 모드에서 Data truncated 로 끊긴다. V009(activity_log.type),
--   V020(notification.type) 과 같은 처리로 VARCHAR 로 맞춘다.
--   PERSONAL / EXTERNAL 두 값 모두 문자열로 그대로 보존된다.
--
-- 안전성:
--   제약 완화라 기존 코드가 동작 중인 DB 에 먼저 적용해도 된다.
--   아직 이 코드를 받지 않은 애플리케이션은 항상 값을 채워 INSERT 하므로 영향이 없다.

ALTER TABLE user_portfolio
    MODIFY COLUMN kind VARCHAR(255) NULL;
