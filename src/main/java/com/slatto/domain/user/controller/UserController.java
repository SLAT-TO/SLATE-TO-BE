package com.slatto.domain.user.controller;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
import com.slatto.domain.user.dto.UserProfileImageResponse;
import com.slatto.domain.user.dto.UserPublicProfileResponse;
import com.slatto.domain.user.dto.UserWithdrawRequest;
import com.slatto.domain.auth.support.AuthCookieFactory;
import com.slatto.domain.user.service.UserService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "유저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthCookieFactory authCookieFactory;

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

    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지를 S3에 업로드하고 CDN 공개 URL로 교체한다.")
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileImageResponse> uploadProfileImage(
        @AuthenticationPrincipal Long userId,
        @RequestPart("file") MultipartFile file
    ) {
        UserProfileImageResponse response = userService.uploadProfileImage(userId, file);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "회원 탈퇴",
        description = """
            계정을 탈퇴 처리한다. 물리 삭제가 아니라 `deleted_at` 기록과 개인정보 익명화로 처리한다.

            프로젝트·공고 등 연관 데이터가 유저를 참조하고 있어 행 자체는 남기며,
            이메일·닉네임·소셜 ID·프로필 이미지·소개는 지운다.

            포트폴리오도 함께 내려가고 리프레시 토큰은 삭제된다.
            이메일이 비워지므로 같은 이메일로 다시 가입하면 새 계정으로 시작한다.
            """
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody UserWithdrawRequest request
    ) {
        userService.withdraw(userId, request);

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshToken().toString())
            .body(ApiResponse.<Void>success(CommonSuccessCode.OK, null));
    }

    @Operation(summary = "공개 프로필 조회", description = "다른 유저의 공개 프로필을 조회한다. 이메일 등 비공개 필드는 제외된다.")
    @GetMapping("/{userId}")
    public ApiResponse<UserPublicProfileResponse> getPublicProfile(@PathVariable Long userId) {
        UserPublicProfileResponse response = userService.getPublicProfile(userId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
