package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.dto.NotificationListResponse;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "알림 목록 조회",
        description = """
            최근 24시간 이내에 갱신된 알림만 반환한다.
            읽지 않은 알림이 먼저 오고, 그 다음은 최근에 갱신된 순이다. cursor 기반 페이지네이션이며 size 는 기본 20이다."""
    )
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "42")
        @RequestParam(required = false) Long cursor,
        @Parameter(description = "조회 개수. 생략 시 20, 최대 50입니다.", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        NotificationListResponse response = notificationService.getNotifications(
            currentUserId,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "알림 단건 읽음 처리",
        description = "본인에게 온 알림만 처리할 수 있다."
    )
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markNotificationAsRead(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long notificationId
    ) {
        notificationService.markNotificationAsRead(currentUserId, notificationId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "알림 전체 읽음 처리",
        description = """
            읽지 않은 알림을 모두 읽음으로 바꾼다.
            목록 조회와 달리 24시간 제한이 없어서, 목록에 보이지 않는 오래된 알림도 함께 처리된다."""
    )
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllNotificationsAsRead(
        @AuthenticationPrincipal Long currentUserId
    ) {
        notificationService.markAllNotificationsAsRead(currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
