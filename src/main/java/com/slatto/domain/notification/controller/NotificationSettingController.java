package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.dto.NotificationSettingResponse;
import com.slatto.domain.notification.dto.NotificationSettingUpdateRequest;
import com.slatto.domain.notification.service.NotificationSettingService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "NotificationSetting", description = "알림 설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "알림 설정 조회", description = "로그인한 유저의 이메일 알림 설정을 조회한다.")
    @GetMapping
    public ApiResponse<NotificationSettingResponse> getMySettings(@AuthenticationPrincipal Long userId) {
        NotificationSettingResponse response = notificationSettingService.getMySettings(userId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "알림 설정 변경", description = "전달된 항목만 부분 수정한다. 미전달 항목은 기존 값을 유지한다.")
    @PatchMapping
    public ApiResponse<NotificationSettingResponse> updateMySettings(
        @AuthenticationPrincipal Long userId,
        @RequestBody NotificationSettingUpdateRequest request
    ) {
        NotificationSettingResponse response = notificationSettingService.updateMySettings(userId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
