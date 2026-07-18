package com.slatto.domain.auth.service;

import com.slatto.domain.auth.client.GoogleOAuthClient;
import com.slatto.domain.auth.client.dto.GoogleTokenResponse;
import com.slatto.domain.auth.client.dto.GoogleUserInfo;
import com.slatto.domain.auth.entity.RefreshToken;
import com.slatto.domain.auth.repository.RefreshTokenRepository;
import com.slatto.domain.auth.support.GoogleAuthFailureReason;
import com.slatto.domain.auth.support.OAuthState;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.config.properties.FrontendProperties;
import com.slatto.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final GoogleOAuthClient googleOAuthClient;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final FrontendProperties frontendProperties;

	public GoogleLoginEntry createGoogleLoginEntry(String redirectTo) {
		OAuthState state = OAuthState.create(frontendProperties.resolveRedirectPath(redirectTo));

		return new GoogleLoginEntry(googleOAuthClient.buildAuthorizationUri(state.value()), state);
	}

	@Transactional
	public GoogleCallbackResult handleGoogleCallback(
		String code,
		String state,
		String error,
		String stateCookieValue
	) {
		if (error != null) {
			return failure(GoogleAuthFailureReason.ACCESS_DENIED);
		}

		OAuthState storedState = stateCookieValue == null ? null : OAuthState.fromCookieValue(stateCookieValue);

		if (code == null || state == null || storedState == null || !storedState.value().equals(state)) {
			return failure(GoogleAuthFailureReason.INVALID_STATE);
		}

		GoogleUserInfo userInfo;

		try {
			GoogleTokenResponse token = googleOAuthClient.exchangeCodeForToken(code);
			userInfo = googleOAuthClient.fetchUserInfo(token.accessToken());
		} catch (Exception exception) {
			log.warn("[Google OAuth] 인가 코드 교환 또는 프로필 조회 실패", exception);
			return failure(GoogleAuthFailureReason.AUTH_FAILED);
		}

		if (userInfo == null || userInfo.email() == null || !userInfo.isEmailVerified()) {
			return failure(GoogleAuthFailureReason.AUTH_FAILED);
		}

		Users user = findOrCreateUser(userInfo);
		String refreshToken = issueRefreshToken(user);

		return new GoogleCallbackResult(
			frontendProperties.toAbsoluteUrl(storedState.redirectPath()),
			refreshToken,
			jwtTokenProvider.refreshTokenMaxAgeSeconds()
		);
	}

	private Users findOrCreateUser(GoogleUserInfo userInfo) {
		return userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, userInfo.sub())
			.or(() -> userRepository.findByEmail(userInfo.email())
				.map(existing -> {
					existing.linkSocialAccount(SocialType.GOOGLE, userInfo.sub());
					return existing;
				}))
			.orElseGet(() -> userRepository.save(Users.createSocialUser(
				userInfo.email(),
				userInfo.name(),
				userInfo.picture(),
				SocialType.GOOGLE,
				userInfo.sub()
			)));
	}

	private String issueRefreshToken(Users user) {
		refreshTokenRepository.deleteByUser(user);

		String token = jwtTokenProvider.createRefreshToken(user.getId());
		refreshTokenRepository.save(RefreshToken.issue(user, token, jwtTokenProvider.refreshTokenExpiresAt()));

		return token;
	}

	private GoogleCallbackResult failure(GoogleAuthFailureReason reason) {
		String redirectUri = frontendProperties.toAbsoluteUrl(frontendProperties.errorPath())
			+ "?reason=" + reason.name();

		return new GoogleCallbackResult(redirectUri, null, 0);
	}

	public record GoogleLoginEntry(String authorizationUri, OAuthState state) {
	}

	public record GoogleCallbackResult(String redirectUri, String refreshToken, long refreshTokenMaxAgeSeconds) {

		public boolean isSuccess() {
			return refreshToken != null;
		}

	}

}
