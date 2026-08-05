package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.MyApplicationListResponse;
import com.slatto.domain.recruitment.dto.MyRecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.recruitment.service.RecruitmentApplicationService;
import com.slatto.domain.recruitment.service.RecruitmentBookmarkService;
import com.slatto.domain.recruitment.service.RecruitmentService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Recruitment", description = "마이페이지 구인구직 목록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MyRecruitmentController {

    private final RecruitmentService recruitmentService;
    private final RecruitmentBookmarkService recruitmentBookmarkService;
    private final RecruitmentApplicationService recruitmentApplicationService;

    @Operation(
        summary = "내가 올린 공고 목록 조회",
        description = "nextCursor 는 공고 ID 기준이다."
    )
    @GetMapping("/recruitments")
    public ApiResponse<MyRecruitmentListResponse> getMyRecruitments(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) RecruitmentStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        MyRecruitmentListResponse response = recruitmentService.getMyRecruitments(
            currentUserId,
            status,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "관심 있는 공고 목록 조회",
        description = "관심 등록 최신순으로 조회한다. nextCursor 는 공고 ID 가 아니라 관심 등록 ID 기준이다."
    )
    @GetMapping("/recruitment-bookmarks")
    public ApiResponse<RecruitmentListResponse> getMyBookmarks(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecruitmentListResponse response = recruitmentBookmarkService.getMyBookmarks(
            currentUserId,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "내가 지원한 공고 목록 조회",
        description = "지원 최신순으로 조회한다. nextCursor 는 공고 ID 가 아니라 지원 ID 기준이다. "
            + "삭제된 공고에 대한 지원은 제외한다."
    )
    @GetMapping("/applications")
    public ApiResponse<MyApplicationListResponse> getMyApplications(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) RecruitmentApplicationStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        MyApplicationListResponse response = recruitmentApplicationService.getMyApplications(
            currentUserId,
            status,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
