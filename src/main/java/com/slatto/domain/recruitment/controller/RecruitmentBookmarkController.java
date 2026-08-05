package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.RecruitmentBookmarkResponse;
import com.slatto.domain.recruitment.service.RecruitmentBookmarkService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment Bookmark", description = "구인구직 공고 관심 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments/{recruitmentId}/bookmark")
public class RecruitmentBookmarkController {

    private final RecruitmentBookmarkService recruitmentBookmarkService;

    @Operation(
        summary = "구인구직 공고 관심 등록",
        description = "멱등하게 동작한다. 이미 등록된 상태에서 다시 호출해도 200 을 반환한다."
    )
    @PostMapping
    public ApiResponse<RecruitmentBookmarkResponse> addBookmark(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        RecruitmentBookmarkResponse response = recruitmentBookmarkService.addBookmark(
            currentUserId,
            recruitmentId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 공고 관심 해제",
        description = "멱등하게 동작한다. 등록되지 않은 상태에서 호출해도 200 을 반환한다."
    )
    @DeleteMapping
    public ApiResponse<RecruitmentBookmarkResponse> removeBookmark(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        RecruitmentBookmarkResponse response = recruitmentBookmarkService.removeBookmark(
            currentUserId,
            recruitmentId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
