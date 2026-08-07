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
        description = "홈 화면에 표시할 오늘의 브리핑을 최대 3건까지 조회합니다. 오늘 일정, 여러 날 일정의 마감 당일, 일정 시작 D-1/D-3 알림과 최근 24시간 주요 알림을 정책 우선순위 기준으로 조합해 반환합니다."
    )
    @GetMapping("/today")
    public ApiResponse<TodayBriefingResponse> getTodayBriefings(
        @AuthenticationPrincipal Long currentUserId
    ) {
        TodayBriefingResponse response = todayBriefingService.getTodayBriefings(currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
