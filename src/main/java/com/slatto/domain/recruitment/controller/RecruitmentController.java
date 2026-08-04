package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.service.RecruitmentService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment", description = "구인구직 공고 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @Operation(summary = "구인구직 공고 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentDetailResponse> createRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody RecruitmentCreateRequest request
    ) {
        RecruitmentDetailResponse response = recruitmentService.createRecruitment(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "구인구직 공고 상세 조회")
    @GetMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> getRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        RecruitmentDetailResponse response = recruitmentService.getRecruitment(currentUserId, recruitmentId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "구인구직 공고 수정")
    @PatchMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @Valid @RequestBody RecruitmentUpdateRequest request
    ) {
        RecruitmentDetailResponse response = recruitmentService.updateRecruitment(
            currentUserId,
            recruitmentId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "구인구직 공고 삭제")
    @DeleteMapping("/{recruitmentId}")
    public ApiResponse<Void> deleteRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        recruitmentService.deleteRecruitment(currentUserId, recruitmentId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
