package com.slatto.domain.auth.controller;

import com.slatto.domain.auth.dto.AccessTokenResponse;
import com.slatto.domain.auth.service.AuthService;
import com.slatto.domain.auth.support.AuthCookieFactory;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookieFactory authCookieFactory;

	@Operation(summary = "구글 로그인 진입", description = "구글 인증 페이지로 302 리다이렉트한다.")
	@GetMapping("/login/google")
	public ResponseEntity<Void> loginWithGoogle(
		@RequestParam(name = "redirectTo", required = false) String redirectTo
	) {
		AuthService.GoogleLoginEntry entry = authService.createGoogleLoginEntry(redirectTo);

		return ResponseEntity
			.status(302)
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.oauthState(entry.state().toCookieValue()).toString())
			.location(URI.create(entry.authorizationUri()))
			.build();
	}

	@Operation(summary = "구글 콜백 처리", description = "구글이 호출하는 엔드포인트로, 프론트엔드는 호출하지 않는다.")
	@GetMapping("/callback/google")
	public ResponseEntity<Void> handleGoogleCallback(
		@RequestParam(name = "code", required = false) String code,
		@RequestParam(name = "state", required = false) String state,
		@RequestParam(name = "error", required = false) String error,
		@CookieValue(name = "${app.cookie.oauth-state-name}", required = false) String stateCookie
	) {
		AuthService.GoogleCallbackResult result = authService.handleGoogleCallback(code, state, error, stateCookie);

		ResponseEntity.BodyBuilder builder = ResponseEntity
			.status(302)
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredOauthState().toString());

		if (result.isSuccess()) {
			builder.header(
				HttpHeaders.SET_COOKIE,
				authCookieFactory.refreshToken(result.refreshToken(), result.refreshTokenMaxAgeSeconds()).toString()
			);
		}

		return builder
			.location(URI.create(result.redirectUri()))
			.build();
	}

	@Operation(summary = "액세스 토큰 재발급", description = "쿠키의 리프레시 토큰으로 새 액세스 토큰을 발급한다.")
	@PostMapping("/refresh")
	public ApiResponse<AccessTokenResponse> reissueAccessToken(
		@CookieValue(name = "${app.cookie.refresh-token-name}", required = false) String refreshToken
	) {
		return ApiResponse.success(CommonSuccessCode.OK, authService.reissueAccessToken(refreshToken));
	}

}
