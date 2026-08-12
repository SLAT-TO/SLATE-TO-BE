package com.slatto.domain.user.controller;

import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
import com.slatto.domain.user.dto.PortfolioDetailResponse;
import com.slatto.domain.user.dto.PortfolioListResponse;
import com.slatto.domain.user.dto.PortfolioUpdateRequest;
import com.slatto.domain.user.dto.PortfolioUpdateResponse;
import com.slatto.domain.user.service.PortfolioService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Portfolio", description = "포트폴리오(프로젝트 이력) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "포트폴리오 목록 조회", description = "특정 유저의 프로젝트 이력 목록을 커서 기반 페이지네이션으로 조회한다.")
    @GetMapping("/{userId}/portfolios")
    public ApiResponse<PortfolioListResponse> getPortfolios(
        @PathVariable Long userId,
        @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "5")
        @RequestParam(required = false) Long cursor,
        @Parameter(description = "조회 개수. 생략 시 10, 최대 50입니다.", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        PortfolioListResponse response = portfolioService.getPortfolios(userId, cursor, size);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "포트폴리오 단건 조회", description = "내 포트폴리오 한 건의 상세 정보를 조회한다.")
    @GetMapping("/me/portfolios/{portfolioId}")
    public ApiResponse<PortfolioDetailResponse> getPortfolio(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long portfolioId
    ) {
        PortfolioDetailResponse response = portfolioService.getPortfolio(currentUserId, portfolioId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "포트폴리오 생성",
        description = """
            프로젝트 이력을 새로 등록한다. 영상 링크에서 썸네일을 자동 추출해 저장한다.

            `kind`(개인/외주 구분)는 선택 입력이라 고르지 않아도 등록된다.
            `PERSONAL` 을 고르면 의뢰자를 입력해도 저장하지 않는다.
            """
    )
    @PostMapping("/me/portfolios")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioCreateResponse> createPortfolio(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody PortfolioCreateRequest request
    ) {
        PortfolioCreateResponse response = portfolioService.createPortfolio(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "포트폴리오 수정", description = "내 포트폴리오를 수정한다. 전달된 항목만 부분 수정되며 roles는 전체 교체된다.")
    @PatchMapping("/me/portfolios/{portfolioId}")
    public ApiResponse<PortfolioUpdateResponse> updatePortfolio(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long portfolioId,
        @Valid @RequestBody PortfolioUpdateRequest request
    ) {
        PortfolioUpdateResponse response = portfolioService.updatePortfolio(currentUserId, portfolioId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "포트폴리오 삭제", description = "내 포트폴리오를 soft delete 한다.")
    @DeleteMapping("/me/portfolios/{portfolioId}")
    public ApiResponse<Void> deletePortfolio(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long portfolioId
    ) {
        portfolioService.deletePortfolio(currentUserId, portfolioId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
