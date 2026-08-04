package com.slatto.domain.recruitment.service;

import com.slatto.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentNotificationDispatcher {

    // Notification.title 은 length 255 이고 알림 제목이 "{공고제목} · 새로운 지원자" 라 공고 제목 + 10자다.
    private static final int MAX_NOTIFICATION_TITLE_LENGTH = 245;

    private final NotificationService notificationService;

    /**
     * 알림 실패가 지원 트랜잭션을 롤백시키지 않도록 별도 트랜잭션으로 격리한다.
     * NotificationService 는 REQUIRED 라 같은 트랜잭션에 참여하는데, 그 안에서 예외가 나면
     * 트랜잭션이 rollback-only 로 마킹돼 호출부에서 catch 해도 커밋 시점에 실패한다.
     * 그룹핑 경로는 더티 체킹이라 SQL 이 커밋 때 나가므로 try 블록으로도 잡히지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchApplied(
        Long writerId,
        Long recruitmentId,
        String recruitmentTitle,
        String applicantName
    ) {
        try {
            notificationService.createRecruitmentAppliedNotification(
                writerId,
                recruitmentId,
                truncateTitle(recruitmentTitle),
                applicantName
            );
        } catch (Exception exception) {
            log.error(
                "[Recruitment] 지원 알림 생성 실패. recruitmentId={}, writerId={}",
                recruitmentId,
                writerId,
                exception
            );
        }
    }

    private String truncateTitle(String title) {
        if (title == null || title.length() <= MAX_NOTIFICATION_TITLE_LENGTH) {
            return title;
        }

        return title.substring(0, MAX_NOTIFICATION_TITLE_LENGTH);
    }
}
