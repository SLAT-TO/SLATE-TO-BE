package com.slatto.domain.user.controller;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.service.UserService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
