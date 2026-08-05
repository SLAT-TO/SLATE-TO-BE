package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.RecruitmentApplicantListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationStatusUpdateRequest;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.service.RecruitmentApplicationService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment Application", description = "구인구직 공고 지원 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments/{recruitmentId}/applications")
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService recruitmentApplicationService;

    @Operation(summary = "구인구직 공고 지원")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentApplicationResponse> applyToRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @Valid @RequestBody RecruitmentApplicationCreateRequest request
    ) {
        RecruitmentApplicationResponse response = recruitmentApplicationService.applyToRecruitment(
            currentUserId,
            recruitmentId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "구인구직 공고 지원자 목록 조회",
        description = "공고 작성자만 조회할 수 있다. nextCursor 는 지원 ID 기준이다."
    )
    @GetMapping
    public ApiResponse<RecruitmentApplicantListResponse> getApplicants(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @RequestParam(required = false) RecruitmentApplicationStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecruitmentApplicantListResponse response = recruitmentApplicationService.getApplicants(
            currentUserId,
            recruitmentId,
            status,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 공고 지원 상태 변경",
        description = "공고 작성자만 변경할 수 있다. PENDING 상태의 지원만 ACCEPTED 또는 REJECTED 로 바꿀 수 있다."
    )
    @PatchMapping("/{applicationId}")
    public ApiResponse<Void> changeApplicationStatus(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitmentApplicationStatusUpdateRequest request
    ) {
        recruitmentApplicationService.changeApplicationStatus(
            currentUserId,
            recruitmentId,
            applicationId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
