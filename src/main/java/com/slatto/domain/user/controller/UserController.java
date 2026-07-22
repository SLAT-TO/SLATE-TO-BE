package com.slatto.domain.user.controller;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
import com.slatto.domain.user.service.UserService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "유저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 정보 조회", description = "로그인한 유저의 기본 정보와 온보딩 완료 여부를 조회한다.")
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        UserMeResponse response = userService.getMyInfo(userId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "온보딩 정보 저장", description = "온보딩에서 입력한 약관 동의·역할·지역·카테고리·프로필 정보를 저장한다. 최초 1회만 허용한다.")
    @PostMapping("/onboarding")
    public ApiResponse<UserOnboardingResponse> completeOnboarding(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody UserOnboardingRequest request
    ) {
        UserOnboardingResponse response = userService.completeOnboarding(userId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로필 수정", description = "마이페이지에서 내 프로필 정보를 수정한다. 전달된 항목만 부분 수정된다.")
    @PatchMapping("/me")
    public ApiResponse<UserProfileUpdateResponse> updateProfile(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileUpdateResponse response = userService.updateProfile(userId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
