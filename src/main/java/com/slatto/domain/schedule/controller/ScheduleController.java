package com.slatto.domain.schedule.controller;

import com.slatto.domain.schedule.dto.ScheduleCalendarResponse;
import com.slatto.domain.schedule.dto.ScheduleCreateRequest;
import com.slatto.domain.schedule.dto.ScheduleDailyResponse;
import com.slatto.domain.schedule.dto.SchedulePrivateMemoRequest;
import com.slatto.domain.schedule.dto.SchedulePrivateMemoResponse;
import com.slatto.domain.schedule.dto.ScheduleResponse;
import com.slatto.domain.schedule.dto.ScheduleUpdateRequest;
import com.slatto.domain.schedule.enums.ScheduleQueryScope;
import com.slatto.domain.schedule.service.ScheduleService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "Schedule", description = "일정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "특정 날짜 일정 조회")
    @GetMapping("/daily")
    public ApiResponse<ScheduleDailyResponse> getDailySchedules(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "ALL") ScheduleQueryScope scope,
        @RequestParam(required = false) Long projectId
    ) {
        ScheduleDailyResponse response = scheduleService.getDailySchedules(
            currentUserId,
            date,
            scope,
            projectId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "전체 일정 조회")
    @GetMapping
    public ApiResponse<ScheduleCalendarResponse> getCalendarSchedules(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(defaultValue = "ALL") ScheduleQueryScope scope,
        @RequestParam(required = false) Long projectId
    ) {
        ScheduleCalendarResponse response = scheduleService.getCalendarSchedules(
            currentUserId,
            startAt,
            endAt,
            scope,
            projectId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "일정 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> createSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody ScheduleCreateRequest request
    ) {
        ScheduleResponse response = scheduleService.createSchedule(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "일정 수정")
    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> updateSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long scheduleId,
        @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        ScheduleResponse response = scheduleService.updateSchedule(currentUserId, scheduleId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "일정 개인 메모 저장/수정")
    @PatchMapping("/{scheduleId}/private-memo")
    public ApiResponse<SchedulePrivateMemoResponse> upsertPrivateMemo(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long scheduleId,
        @Valid @RequestBody SchedulePrivateMemoRequest request
    ) {
        SchedulePrivateMemoResponse response = scheduleService.upsertPrivateMemo(
            currentUserId,
            scheduleId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "일정 삭제")
    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long scheduleId
    ) {
        scheduleService.deleteSchedule(currentUserId, scheduleId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
