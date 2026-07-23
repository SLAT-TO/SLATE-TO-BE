package com.slatto.domain.user.controller;

import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
import com.slatto.domain.user.service.PortfolioService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Portfolio", description = "포트폴리오(프로젝트 이력) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "포트폴리오 생성", description = "프로젝트 이력을 새로 등록한다. 영상 링크에서 썸네일을 자동 추출해 저장한다.")
    @PostMapping("/me/portfolios")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioCreateResponse> createPortfolio(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody PortfolioCreateRequest request
    ) {
        PortfolioCreateResponse response = portfolioService.createPortfolio(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }
}
