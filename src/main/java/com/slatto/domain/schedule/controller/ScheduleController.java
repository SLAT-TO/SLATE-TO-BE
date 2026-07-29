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
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
        summary = "특정 날짜 일정 조회",
        description = "선택한 날짜에 표시할 일정 목록을 조회합니다. 응답에는 일정 대상자, 공용 메모, 로그인한 사용자의 개인 메모, 수정 가능 여부가 포함됩니다."
    )
    @GetMapping("/daily")
    public ApiResponse<ScheduleDailyResponse> getDailySchedules(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "조회할 날짜", example = "2026-07-05")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Parameter(description = "조회 범위. ALL, PERSONAL, PROJECT 중 하나를 전달합니다.", example = "ALL")
        @RequestParam(defaultValue = "ALL") ScheduleQueryScope scope,
        @Parameter(description = "특정 프로젝트 일정만 조회할 때 전달합니다.", example = "3")
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

    @Operation(
        summary = "통합 캘린더 일정 조회",
        description = "캘린더에 표시할 일정 목록을 기간 기준으로 조회합니다. 프로젝트 캘린더 조회 시 scope=PROJECT와 projectId를 함께 전달합니다."
    )
    @GetMapping
    public ApiResponse<ScheduleCalendarResponse> getCalendarSchedules(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "조회 시작 일시", example = "2026-07-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @Parameter(description = "조회 종료 일시", example = "2026-07-31T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @Parameter(description = "조회 범위. ALL, PERSONAL, PROJECT 중 하나를 전달합니다. 프로젝트 캘린더 조회 시 PROJECT로 전달합니다.", example = "ALL")
        @RequestParam(defaultValue = "ALL") ScheduleQueryScope scope,
        @Parameter(description = "특정 프로젝트 일정만 조회할 때 전달합니다. 프로젝트 캘린더 조회 시 scope=PROJECT와 함께 전달합니다.", example = "3")
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

    @Operation(
        summary = "일정 생성",
        description = "개인 일정 또는 프로젝트 일정을 생성합니다. PERSONAL 일정은 projectId와 participantIds를 전달하지 않고, PROJECT 일정은 projectId와 participantIds가 필요합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> createSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody ScheduleCreateRequest request
    ) {
        ScheduleResponse response = scheduleService.createSchedule(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "일정 수정",
        description = "일정 생성자만 일정을 수정할 수 있습니다. 전달된 항목만 부분 수정되며, 프로젝트 일정의 participantIds를 전달하지 않으면 기존 대상자를 유지하고 빈 배열을 전달하면 모든 대상자를 제거합니다."
    )
    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> updateSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "수정할 일정 ID", example = "10")
        @PathVariable Long scheduleId,
        @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        ScheduleResponse response = scheduleService.updateSchedule(currentUserId, scheduleId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "일정 개인 메모 저장/수정",
        description = "로그인한 사용자가 볼 수 있는 일정에 대해 나에게만 보이는 개인 메모를 저장하거나 수정합니다."
    )
    @PatchMapping("/{scheduleId}/private-memo")
    public ApiResponse<SchedulePrivateMemoResponse> upsertPrivateMemo(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "개인 메모를 저장하거나 수정할 일정 ID", example = "10")
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

    @Operation(
        summary = "일정 삭제",
        description = "일정 생성자만 일정을 삭제할 수 있습니다. 일정, 일정 대상자, 개인 메모는 soft delete 처리됩니다."
    )
    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "삭제할 일정 ID", example = "10")
        @PathVariable Long scheduleId
    ) {
        scheduleService.deleteSchedule(currentUserId, scheduleId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
