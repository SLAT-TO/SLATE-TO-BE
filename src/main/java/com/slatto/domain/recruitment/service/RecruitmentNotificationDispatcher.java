package com.slatto.domain.recruitment.service;

import com.slatto.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecruitmentNotificationDispatcher {

    // Notification.title 은 length 255 이고 알림 제목이 "{공고제목} · 새로운 지원자" 라 공고 제목 + 10자다.
    private static final int MAX_NOTIFICATION_TITLE_LENGTH = 245;

    private final NotificationService notificationService;

    /**
     * 알림 실패가 지원 트랜잭션을 롤백시키지 않도록 별도 트랜잭션으로 격리한다.
     * NotificationService 는 REQUIRED 라 그대로 호출하면 지원 트랜잭션에 참여하고,
     * 그 안에서 예외가 나면 rollback-only 로 마킹돼 지원까지 함께 롤백된다.
     *
     * 여기서 예외를 잡지 않는다. 트랜잭션 커밋은 이 메서드가 리턴한 뒤 프록시에서 일어나므로
     * 메서드 안의 try/catch 로는 커밋 시점 예외를 잡을 수 없다. 호출부에서 잡아야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchApplied(
        Long writerId,
        Long recruitmentId,
        String recruitmentTitle,
        String applicantName
    ) {
        notificationService.createRecruitmentAppliedNotification(
            writerId,
            recruitmentId,
            truncateTitle(recruitmentTitle),
            applicantName
        );
    }

    private String truncateTitle(String title) {
        if (title == null || title.length() <= MAX_NOTIFICATION_TITLE_LENGTH) {
            return title;
        }

        return title.substring(0, MAX_NOTIFICATION_TITLE_LENGTH);
    }
}
