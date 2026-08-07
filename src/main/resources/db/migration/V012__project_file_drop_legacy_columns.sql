-- project_file 레거시 컬럼 제거 (file_url, is_pinned)
--
-- 배경:
--   a833792(2026-07-23) "프로젝트 파일 도메인 모델 정리"에서 ProjectFile 엔티티의
--   file_url -> storage_key, isPinned(boolean) -> pinnedAt(nullable) 으로 모델을 바꿨으나
--   대응 마이그레이션이 없었다. 운영 DB는 ddl-auto=update로 생성돼 신규 컬럼만 추가되고
--   구 컬럼 두 개가 NOT NULL / DEFAULT 없음 상태로 남았다.
--   현재 INSERT문은 두 컬럼을 포함하지 않으므로 파일 업로드가 전건 실패한다.
--     Field 'file_url' doesn't have a default value
--   ddl-auto=validate는 "엔티티에 있는데 DB에 없는 컬럼"만 검사하고 그 반대는 보지 않아
--   이 상태를 잡아내지 못했다.
--
-- 안전성:
--   삭제 시점 기준 project_file 행 수 0건이므로 데이터 손실 없음.
--
-- 주의:
--   MySQL 8.4에는 DROP COLUMN IF EXISTS가 없다(MariaDB 전용 문법). 조건 없이 삭제한다.
--   따라서 두 컬럼이 존재하는 DB에서만 성공한다. 운영 DB에서 직접 DROP을 먼저 실행하면
--   이 마이그레이션이 ERROR 1091로 실패해 이후 배포가 모두 막히므로 절대 병행하지 말 것.
--
-- 버전 번호:
--   V011은 d5191f6에서 activity_log 인덱스용으로 쓰였다가 f1e99f0에서 삭제된 이력이 있어
--   재사용하지 않는다. 번호를 비워도 Flyway 동작에는 영향이 없다.
ALTER TABLE project_file
    DROP COLUMN file_url,
    DROP COLUMN is_pinned;
