package com.slatto.domain.notification.dto;

import com.slatto.domain.notification.enums.NotificationTargetType;
import com.slatto.domain.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationCreateCommand {

    /** 알림을 받을 사용자 ID 목록 */
    private List<Long> recipientIds;

    /** 프로젝트 맥락이 있는 알림이면 프로젝트 ID, 구인구직처럼 프로젝트와 무관하면 null */
    private Long projectId;

    /** 알림센터에서 구분할 알림 종류 */
    private NotificationType type;

    /** 알림센터에 표시할 문구 */
    private String content;

    /** 알림 클릭 시 이동할 대상 종류 */
    private NotificationTargetType targetType;

    /** 알림 클릭 시 이동할 대상 ID */
    private Long targetId;

    /** 알림 생성 대상에서 제외할 사용자 ID */
    private Long excludeUserId;
}
