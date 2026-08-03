package com.slatto.domain.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityMessageFactoryTest {

    private final ActivityMessageFactory activityMessageFactory = new ActivityMessageFactory();

    @Test
    void 확정된_최근활동_문구를_일관되게_생성한다() {
        // 각 도메인이 문구를 직접 조합하지 않아도 PM 합의 문구가 유지되는지 확인한다.
        assertThat(activityMessageFactory.projectMemberJoined("김수민"))
            .isEqualTo("김수민님이 프로젝트에 합류했어요");
        assertThat(activityMessageFactory.projectStatusChanged("김수민", "준비중", "편집중"))
            .isEqualTo("김수민님이 프로젝트 단계를 '준비중'에서 '편집중'으로 변경했어요");
        assertThat(activityMessageFactory.projectUpdated("김수민"))
            .isEqualTo("김수민님이 프로젝트 정보를 수정했어요");
        assertThat(activityMessageFactory.scheduleCreated("김수민", "촬영 회의"))
            .isEqualTo("김수민님이 [촬영 회의] 일정을 등록했어요");
        assertThat(activityMessageFactory.scheduleUpdated("김수민", "촬영 회의"))
            .isEqualTo("김수민님이 [촬영 회의] 일정을 수정했어요");
        assertThat(activityMessageFactory.noticeCreated("김수민"))
            .isEqualTo("김수민님이 새 공지를 등록했어요");
        assertThat(activityMessageFactory.fileUploaded("김수민", "촬영계획서.pdf"))
            .isEqualTo("김수민님이 [촬영계획서.pdf] 파일을 등록했어요");
        assertThat(activityMessageFactory.videoFeedbackCommented("김수민", "버전 1"))
            .isEqualTo("김수민님이 [버전 1]에 피드백을 남겼어요");
    }
}
