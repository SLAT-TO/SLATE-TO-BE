package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.dto.ActivityLogListResponse;
import com.slatto.domain.notification.service.RecentActivityService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recent Activity", description = "프로젝트 최근활동 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/activities")
public class RecentActivityController {

    private final RecentActivityService recentActivityService;

    @Operation(summary = "프로젝트 최근활동 목록 조회")
    @GetMapping
    public ApiResponse<ActivityLogListResponse> getRecentActivities(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ActivityLogListResponse response = recentActivityService.getRecentActivities(
            projectId,
            currentUserId,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 최근활동 개별 확인")
    @PatchMapping("/{activityId}/read")
    public ApiResponse<Void> markActivityAsRead(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long activityId
    ) {
        recentActivityService.markActivityAsRead(projectId, activityId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(summary = "프로젝트 최근활동 전체 확인")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllActivitiesAsRead(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        recentActivityService.markAllActivitiesAsRead(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
