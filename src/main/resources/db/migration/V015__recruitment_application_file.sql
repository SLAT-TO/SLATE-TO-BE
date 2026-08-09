-- 공고 지원 첨부 파일 테이블 추가
--
-- application_id 가 nullable 인 이유:
--   지원 API 는 마감·중복 지원으로 실패할 수 있어 multipart 로 한 번에 받으면 실패할 때마다
--   파일을 다시 올려야 한다. 그래서 업로드를 먼저 하고 반환된 id 로 지원 요청을 보낸다.
--   업로드 시점에는 아직 지원 행이 없으므로 NULL 로 두고, 지원 성공 시점에 연결한다.
--
-- uploader_id 를 따로 두는 이유:
--   application_id 가 NULL 인 구간에는 소유자를 판정할 근거가 이 컬럼뿐이다.
--   남이 올린 파일 id 를 자기 지원에 붙이는 것을 막는 검증에 쓴다.
--
-- 공개 URL 을 만들지 않고 storage_key 만 저장한다. 지원 서류는 이력서 성격이라
-- URL 만 알면 접근 가능한 상태로 두면 안 된다. 다운로드 API 에서 권한을 검사한다.

CREATE TABLE recruitment_application_file
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    recruitment_id BIGINT       NOT NULL,
    application_id BIGINT       NULL,
    uploader_id    BIGINT       NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(150) NOT NULL,
    file_size      BIGINT       NOT NULL,
    storage_key    VARCHAR(500) NOT NULL,
    deleted_at     DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recruitment_application_file_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES recruitment (id),
    CONSTRAINT fk_recruitment_application_file_application
        FOREIGN KEY (application_id) REFERENCES recruitment_application (id),
    CONSTRAINT fk_recruitment_application_file_uploader
        FOREIGN KEY (uploader_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- application_id / uploader_id 조회용 인덱스를 따로 만들지 않는다.
-- InnoDB 는 FK 제약에 인덱스가 필요해 없으면 자동으로 생성하므로, 같은 컬럼에 CREATE INDEX 를
-- 더하면 완전히 중복된 인덱스가 하나 더 생겨 INSERT/UPDATE 비용만 늘어난다.
