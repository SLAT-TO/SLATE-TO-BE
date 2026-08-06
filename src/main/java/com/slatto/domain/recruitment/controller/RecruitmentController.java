package com.slatto.domain.recruitment.controller;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentRecommendationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.enums.RecruitmentSortType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.recruitment.service.RecruitmentService;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
        summary = "구인구직 공고 목록 조회",
        description = "필터는 AND 로 결합한다. nextCursor 는 공고 ID 이며 프론트는 해석하지 않고 그대로 되돌려준다. "
            + "정렬이나 필터가 바뀌면 cursor 를 버리고 첫 페이지부터 다시 조회해야 한다."
    )
    @GetMapping
    public ApiResponse<RecruitmentListResponse> getRecruitments(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) CategoryName category,
        @RequestParam(required = false) LengthType lengthType,
        @RequestParam(required = false) RoleName recruitPart,
        @RequestParam(required = false) RegionName location,
        @RequestParam(required = false) RecruitmentStatus status,
        @RequestParam(defaultValue = "LATEST") RecruitmentSortType sort,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecruitmentListResponse response = recruitmentService.getRecruitments(
            currentUserId,
            keyword,
            category,
            lengthType,
            recruitPart,
            location,
            status,
            sort,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 추천 공고 조회",
        description = "활동 역할·관심 카테고리·활동 지역 매칭도 상위 공고 중에서 무작위로 고른다. "
            + "호출마다 조합이 달라지며 페이지네이션이 없다."
    )
    @GetMapping("/recommended")
    public ApiResponse<RecruitmentRecommendationResponse> getRecommendedRecruitments(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(defaultValue = "4") int size
    ) {
        RecruitmentRecommendationResponse response =
            recruitmentService.getRecommendedRecruitments(currentUserId, size);

        return ApiResponse.success(CommonSuccessCode.OK, response);
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
