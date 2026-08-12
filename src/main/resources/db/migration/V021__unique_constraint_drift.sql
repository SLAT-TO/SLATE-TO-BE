-- notification_setting.user_id 와 share_link.video_id 는 엔티티에 unique = true 로 선언돼 있지만
-- 운영 DB 에는 일반 인덱스로만 존재한다.
--
-- 두 컬럼 모두 테이블이 만들어진 뒤에 unique 가 추가됐다(5120f88, 5f583df). 당시는 Flyway 도입 전이라
-- 스키마 반영 수단이 ddl-auto 뿐이었는데, ddl-auto 는 없는 테이블·컬럼만 만들고 이미 있는 컬럼에
-- 제약을 덧붙이지 않는다. 그래서 매번 새로 만드는 로컬에는 걸리고 운영에는 영영 걸리지 않았다.
--
-- 제약이 없으면 동시 요청으로 행이 둘 생길 수 있다. 두 리포지터리 모두 Optional 로 조회하므로
-- (NotificationSettingRepository.findByUserId, ShareLinkRepository.findByVideoId)
-- 중복이 한 번 생기면 그 사용자·영상은 이후 조회할 때마다 500 이 난다.

ALTER TABLE notification_setting
    ADD CONSTRAINT uq_notification_setting_user UNIQUE (user_id);

ALTER TABLE share_link
    ADD CONSTRAINT uq_share_link_video UNIQUE (video_id);
