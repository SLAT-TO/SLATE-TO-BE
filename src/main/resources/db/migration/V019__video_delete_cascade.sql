-- 영상은 피드백, 공유 링크, 북마크, 참조 파일 연결의 생명주기 루트다.
-- 영상 삭제 시 종속 행이 남아 FK 위반으로 500이 발생하지 않도록 삭제 규칙을 명시한다.

ALTER TABLE feedback_detail
    DROP FOREIGN KEY FK9kdj80tqen9e7nbnj13vnk661;
ALTER TABLE feedback_detail
    ADD CONSTRAINT FK9kdj80tqen9e7nbnj13vnk661
        FOREIGN KEY (feedback_id) REFERENCES feedback (id) ON DELETE CASCADE;

ALTER TABLE feedback
    DROP FOREIGN KEY FKejspu7a0b6480ufbmbyb3k1g1;
ALTER TABLE feedback
    ADD CONSTRAINT FKejspu7a0b6480ufbmbyb3k1g1
        FOREIGN KEY (video_id) REFERENCES video (id) ON DELETE CASCADE;

ALTER TABLE guest
    DROP FOREIGN KEY FK4l013yp9w51s1lo4fyp08pxfl;
ALTER TABLE guest
    ADD CONSTRAINT FK4l013yp9w51s1lo4fyp08pxfl
        FOREIGN KEY (share_link_id) REFERENCES share_link (id) ON DELETE CASCADE;

ALTER TABLE share_link
    DROP FOREIGN KEY FKho2bh2ntuoln19dgyjap6bxm3;
ALTER TABLE share_link
    ADD CONSTRAINT FKho2bh2ntuoln19dgyjap6bxm3
        FOREIGN KEY (video_id) REFERENCES video (id) ON DELETE CASCADE;

ALTER TABLE video_bookmark
    DROP FOREIGN KEY FKjqx3o5s07mw82wvcdursgq1mj;
ALTER TABLE video_bookmark
    ADD CONSTRAINT FKjqx3o5s07mw82wvcdursgq1mj
        FOREIGN KEY (video_id) REFERENCES video (id) ON DELETE CASCADE;

ALTER TABLE video_reference_file
    DROP FOREIGN KEY FKmv871ptaholpgyt22fp0n3s2p;
ALTER TABLE video_reference_file
    ADD CONSTRAINT FKmv871ptaholpgyt22fp0n3s2p
        FOREIGN KEY (video_id) REFERENCES video (id) ON DELETE CASCADE;
