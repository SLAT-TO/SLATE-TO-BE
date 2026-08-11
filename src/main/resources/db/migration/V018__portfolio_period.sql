-- 포트폴리오에 참여 기간 컬럼 추가
--
-- 프로젝트를 완료로 전환하면 참여자 전원의 포트폴리오가 자동 생성되는데,
-- 이때 프로젝트 기간을 그대로 담을 자리가 필요해 추가한다.
--
-- NULL 을 허용하는 이유:
--   이미 등록된 포트폴리오에는 기간이 없고, 직접 등록하는 경우에도 선택 입력이다.
--   기간을 모르는 예전 작업을 이력에 남기지 못하게 막을 이유가 없다.

ALTER TABLE user_portfolio
    ADD COLUMN start_date DATE NULL,
    ADD COLUMN end_date   DATE NULL;
