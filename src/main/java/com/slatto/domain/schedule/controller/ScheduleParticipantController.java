package com.slatto.domain.schedule.controller;

import com.slatto.domain.schedule.dto.ScheduleParticipantCandidateResponse;
import com.slatto.domain.schedule.service.ScheduleService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Schedule Participant", description = "일정 대상자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/schedule-participants")
public class ScheduleParticipantController {

    private static final String CURRENT_USER_ID_HEADER = "X-USER-ID";

    private final ScheduleService scheduleService;

    @Operation(summary = "프로젝트 일정 대상자 후보 조회")
    @GetMapping("/candidates")
    public ApiResponse<ScheduleParticipantCandidateResponse> getParticipantCandidates(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId
    ) {
        ScheduleParticipantCandidateResponse response = scheduleService.getParticipantCandidates(
            currentUserId,
            projectId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
