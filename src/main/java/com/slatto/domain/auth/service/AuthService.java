package com.slatto.domain.auth.service;

import com.slatto.domain.auth.client.GoogleOAuthClient;
import com.slatto.domain.auth.client.dto.GoogleTokenResponse;
import com.slatto.domain.auth.client.dto.GoogleUserInfo;
import com.slatto.domain.auth.dto.AccessTokenResponse;
import com.slatto.domain.auth.entity.RefreshToken;
import com.slatto.domain.auth.enums.VerificationPurpose;
import com.slatto.domain.auth.exception.AuthErrorCode;
import com.slatto.domain.auth.repository.RefreshTokenRepository;
import com.slatto.domain.auth.support.GoogleAuthFailureReason;
import com.slatto.domain.auth.support.OAuthState;
import com.slatto.domain.notification.entity.NotificationSetting;
import com.slatto.domain.notification.repository.NotificationSettingRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.config.properties.FrontendProperties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	// 존재하지 않는 이메일이면 해시 비교를 건너뛰어 응답이 빨라진다. 그 시간 차이만으로 가입 여부가 드러나므로
	// 항상 한 번은 비교한다. 이 값은 결과가 버려지는 자리에만 쓴다.
	private static final String DUMMY_PASSWORD_HASH =
		"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final GoogleOAuthClient googleOAuthClient;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final EmailVerificationService emailVerificationService;
	private final PasswordEncoder passwordEncoder;
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

	// 인증 확인을 중복 검사보다 먼저 한다. 순서를 뒤집으면 인증 없이 아무 이메일이나 넣어보고
	// 409 인지 400 인지로 가입 여부를 알아낼 수 있다.
	@Transactional
	public EmailAuthResult signup(String name, String email, String rawPassword) {
		emailVerificationService.consumeVerified(email, VerificationPurpose.SIGNUP);

		userRepository.findByEmail(email).ifPresent(existing -> {
			throw new BaseException(existing.hasPassword()
				? AuthErrorCode.SIGNUP_DUPLICATE_EMAIL
				: AuthErrorCode.SIGNUP_SOCIAL_ACCOUNT_EXISTS);
		});

		Users user = userRepository.save(
			Users.createEmailUser(email, name, passwordEncoder.encode(rawPassword))
		);
		notificationSettingRepository.save(NotificationSetting.createDefault(user));

		return toEmailAuthResult(user);
	}

	@Transactional
	public EmailAuthResult login(String email, String rawPassword) {
		Users user = userRepository.findByEmail(email)
			.filter(it -> it.getDeletedAt() == null)
			.orElse(null);

		boolean hasPassword = user != null && user.hasPassword();
		boolean matched = passwordEncoder.matches(
			rawPassword,
			hasPassword ? user.getPassword() : DUMMY_PASSWORD_HASH
		);

		// 미존재·비밀번호 불일치·소셜 전용 계정을 구분하지 않는다.
		// "구글로 가입된 계정입니다" 같은 안내는 이메일 열거를 그대로 허용한다.
		if (!hasPassword || !matched) {
			throw new BaseException(AuthErrorCode.LOGIN_FAILED);
		}

		return toEmailAuthResult(user);
	}

	@Transactional
	public void resetPassword(String email, String newRawPassword) {
		emailVerificationService.consumeVerified(email, VerificationPurpose.PASSWORD_RESET);

		// 계정이 없으면 인증 메일 자체가 나가지 않으므로 여기까지 올 수 없다. 방어적으로 같은 코드를 쓴다.
		Users user = userRepository.findByEmail(email)
			.filter(it -> it.getDeletedAt() == null)
			.orElseThrow(() -> new BaseException(AuthErrorCode.EMAIL_NOT_VERIFIED));

		user.changePassword(passwordEncoder.encode(newRawPassword));

		// 비밀번호가 유출돼 재설정하는 상황을 가정한다. 살아 있는 세션을 끊지 않으면 의미가 없다.
		refreshTokenRepository.deleteByUser(user);
	}

	// TODO: 리프레시 토큰 회전(rotation) 도입 시 여기서 기존 토큰을 폐기하고 새 토큰을 발급해
	//       AccessTokenResponse와 함께 Set-Cookie로 다시 내려줘야 한다.
	@Transactional(readOnly = true)
	public AccessTokenResponse reissueAccessToken(String refreshTokenValue) {
		if (refreshTokenValue == null) {
			throw new BaseException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
			.orElseThrow(() -> new BaseException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		Long userId = jwtTokenProvider.parseUserId(refreshTokenValue, true);

		if (userId == null || storedToken.isExpired(LocalDateTime.now())) {
			throw new BaseException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		return new AccessTokenResponse(jwtTokenProvider.createAccessToken(userId));
	}

	@Transactional
	public void logout(String refreshTokenValue) {
		if (refreshTokenValue != null) {
			refreshTokenRepository.deleteByToken(refreshTokenValue);
		}
	}

	private Users findOrCreateUser(GoogleUserInfo userInfo) {
		return userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, userInfo.sub())
			.or(() -> userRepository.findByEmail(userInfo.email())
				.map(existing -> {
					existing.linkSocialAccount(SocialType.GOOGLE, userInfo.sub());
					return existing;
				}))
			.orElseGet(() -> {
				Users createdUser = userRepository.save(Users.createSocialUser(
					userInfo.email(),
					userInfo.name(),
					userInfo.picture(),
					SocialType.GOOGLE,
					userInfo.sub()
				));
				notificationSettingRepository.save(NotificationSetting.createDefault(createdUser));
				return createdUser;
			});
	}

	private EmailAuthResult toEmailAuthResult(Users user) {
		return new EmailAuthResult(
			user.getId(),
			jwtTokenProvider.createAccessToken(user.getId()),
			user.getOnboardingCompleted(),
			issueRefreshToken(user),
			jwtTokenProvider.refreshTokenMaxAgeSeconds()
		);
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

	public record EmailAuthResult(
		Long userId,
		String accessToken,
		Boolean onboardingCompleted,
		String refreshToken,
		long refreshTokenMaxAgeSeconds
	) {
	}

	public record GoogleCallbackResult(String redirectUri, String refreshToken, long refreshTokenMaxAgeSeconds) {

		public boolean isSuccess() {
			return refreshToken != null;
		}

	}

}
