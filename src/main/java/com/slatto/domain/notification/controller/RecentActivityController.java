package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.dto.ActivityLogListResponse;
import com.slatto.domain.notification.service.RecentActivityService;
import com.slatto.global.config.ApiErrorCodes;
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

@Tag(name = "Recent Activity", description = "프로젝트 최근활동 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/activities")
public class RecentActivityController {

    private final RecentActivityService recentActivityService;

    @Operation(
        summary = "프로젝트 최근활동 목록 조회",
        description = """
            프로젝트 멤버만 조회할 수 있다. 각 항목에 내가 읽었는지 여부가 함께 담긴다.
            cursor 는 응답의 nextCursor 를 그대로 넘기는 문자열이며 size 는 기본 20, 최대 50이다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @GetMapping
    public ApiResponse<ActivityLogListResponse> getRecentActivities(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @Parameter(description = "이전 응답의 nextCursor 를 그대로 넣습니다. {발생일시}_{활동ID} 형식입니다. 첫 페이지에서는 생략합니다.", example = "2026-08-11T14:30:00_57")
        @RequestParam(required = false) String cursor,
        @Parameter(description = "조회 개수. 생략 시 20, 최대 50입니다.", example = "20")
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

    @Operation(
        summary = "프로젝트 최근활동 단건 읽음 처리",
        description = "읽음은 호출한 사람에게만 기록된다. 이미 읽은 활동에 다시 호출해도 실패하지 않는다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @PatchMapping("/{activityId}/read")
    public ApiResponse<Void> markActivityAsRead(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long activityId
    ) {
        recentActivityService.markActivityAsRead(projectId, activityId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "프로젝트 최근활동 전체 읽음 처리",
        description = "해당 프로젝트의 활동만 읽음 처리한다. 다른 프로젝트에는 영향이 없다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllActivitiesAsRead(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        recentActivityService.markAllActivitiesAsRead(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
