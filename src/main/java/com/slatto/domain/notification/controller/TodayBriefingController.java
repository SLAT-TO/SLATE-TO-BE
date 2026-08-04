package com.slatto.domain.notification.controller;

import com.slatto.domain.notification.dto.TodayBriefingResponse;
import com.slatto.domain.notification.service.TodayBriefingService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Briefing", description = "오늘의 브리핑 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/briefings")
public class TodayBriefingController {

    private final TodayBriefingService todayBriefingService;

    @Operation(
        summary = "오늘의 브리핑 조회",
        description = "홈 화면에 표시할 오늘의 브리핑을 최대 3건까지 조회합니다. 브리핑은 일정과 최근 주요 알림 데이터를 조합해 반환합니다."
    )
    @GetMapping("/today")
    public ApiResponse<TodayBriefingResponse> getTodayBriefings(
        @AuthenticationPrincipal Long currentUserId
    ) {
        TodayBriefingResponse response = todayBriefingService.getTodayBriefings(currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
