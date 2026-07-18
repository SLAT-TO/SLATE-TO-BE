package com.slatto.domain.auth.controller;

import com.slatto.domain.auth.service.AuthService;
import com.slatto.domain.auth.support.AuthCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

}
