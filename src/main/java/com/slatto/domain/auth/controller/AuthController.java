package com.slatto.domain.auth.controller;

import com.slatto.domain.auth.dto.AccessTokenResponse;
import com.slatto.domain.auth.dto.EmailVerificationConfirmRequest;
import com.slatto.domain.auth.dto.EmailVerificationConfirmResponse;
import com.slatto.domain.auth.dto.EmailVerificationSendRequest;
import com.slatto.domain.auth.dto.EmailVerificationSendResponse;
import com.slatto.domain.auth.service.AuthService;
import com.slatto.domain.auth.service.EmailVerificationService;
import com.slatto.domain.auth.support.AuthCookieFactory;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;
	private final AuthCookieFactory authCookieFactory;

	@Operation(
		summary = "구글 로그인 진입",
		description = """
			구글 인증 페이지로 302 리다이렉트한다. 인증이 필요 없다.

			`<a href>` 또는 `window.location.href`로 **브라우저를 이동시켜** 호출한다.
			fetch/ajax로 호출하는 API가 아니며, Swagger의 Try it out으로는 동작하지 않는다.

			성공 시 302로 응답하며, 공통 응답 wrapper를 사용하지 않는다.
			"""
	)
	@SecurityRequirements
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

	@Hidden
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

	@Operation(
		summary = "액세스 토큰 재발급",
		description = "쿠키의 리프레시 토큰으로 새 액세스 토큰을 발급한다. 요청 본문과 Authorization 헤더가 모두 필요 없다."
	)
	@SecurityRequirements
	@PostMapping("/refresh")
	public ApiResponse<AccessTokenResponse> reissueAccessToken(
		@CookieValue(name = "${app.cookie.refresh-token-name}", required = false) String refreshToken
	) {
		return ApiResponse.success(CommonSuccessCode.OK, authService.reissueAccessToken(refreshToken));
	}

	@Operation(
		summary = "이메일 인증번호 발송",
		description = """
			입력한 이메일로 6자리 인증번호를 발송한다. 인증번호는 5분간 유효하다.

			회원가입(`SIGNUP`)과 비밀번호 재설정(`PASSWORD_RESET`)이 같은 엔드포인트를 쓰며 `purpose`로 구분한다.

			**가입 여부와 무관하게 항상 성공을 반환한다.** 계정 존재 여부를 노출하면 아무 이메일이나
			넣어보고 가입 여부를 알아낼 수 있기 때문이다.
			"""
	)
	@SecurityRequirements
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/email/verification-codes")
	public ApiResponse<EmailVerificationSendResponse> sendEmailVerificationCode(
		@Valid @RequestBody EmailVerificationSendRequest request
	) {
		EmailVerificationSendResponse response = emailVerificationService.send(request.email(), request.purpose());

		return ApiResponse.success(CommonSuccessCode.CREATED, response);
	}

	@Operation(
		summary = "이메일 인증번호 확인",
		description = """
			인증번호를 확인하고 해당 이메일을 인증 완료 상태로 만든다.
			성공 후 30분 안에 회원가입 또는 비밀번호 재설정을 마쳐야 한다.

			불일치·만료·시도 횟수 초과를 구분하지 않고 같은 코드로 응답한다.
			"""
	)
	@SecurityRequirements
	@PostMapping("/email/verification-codes/confirm")
	public ApiResponse<EmailVerificationConfirmResponse> confirmEmailVerificationCode(
		@Valid @RequestBody EmailVerificationConfirmRequest request
	) {
		EmailVerificationConfirmResponse response = emailVerificationService.confirm(
			request.email(), request.code(), request.purpose()
		);

		return ApiResponse.success(CommonSuccessCode.OK, response);
	}

	@Operation(summary = "로그아웃", description = "서버에 저장된 리프레시 토큰을 무효화하고 쿠키를 삭제한다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@CookieValue(name = "${app.cookie.refresh-token-name}", required = false) String refreshToken
	) {
		authService.logout(refreshToken);

		return ResponseEntity
			.ok()
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshToken().toString())
			.body(ApiResponse.<Void>success(CommonSuccessCode.OK, null));
	}

}
