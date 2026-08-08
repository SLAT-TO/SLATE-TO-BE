package com.slatto.domain.auth.service;

import com.slatto.domain.auth.client.GoogleOAuthClient;
import com.slatto.domain.auth.exception.AuthErrorCode;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.config.properties.FrontendProperties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import({AuthService.class, PasswordChangeTest.PasswordEncoderTestConfig.class})
@TestPropertySource(properties = {
	"spring.jpa.database=h2",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
class PasswordChangeTest {

	private static final String CURRENT_PASSWORD = "slatto!2026";
	private static final String NEW_PASSWORD = "slatto@2027";

	@TestConfiguration
	static class PasswordEncoderTestConfig {

		// 실제 BCrypt 를 쓴다. 목으로 대체하면 현재 비밀번호 검증이 통과하는지 확인할 수 없다.
		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}
	}

	@MockitoBean
	private GoogleOAuthClient googleOAuthClient;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private FrontendProperties frontendProperties;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	private Long emailUserId;
	private Long socialOnlyUserId;

	@BeforeEach
	void setUp() {
		given(jwtTokenProvider.createAccessToken(org.mockito.ArgumentMatchers.anyLong()))
			.willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(org.mockito.ArgumentMatchers.anyLong()))
			.willReturn("refresh-token");
		given(jwtTokenProvider.refreshTokenExpiresAt())
			.willReturn(LocalDateTime.now().plusDays(14));

		Users emailUser = userRepository.save(
			Users.createEmailUser("email@slatto.com", "이메일유저", passwordEncoder.encode(CURRENT_PASSWORD))
		);
		emailUserId = emailUser.getId();

		Users socialOnlyUser = userRepository.save(
			Users.createSocialUser("social@slatto.com", "소셜유저", null, SocialType.GOOGLE, "social-1")
		);
		socialOnlyUserId = socialOnlyUser.getId();

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("현재 비밀번호가 맞으면 새 비밀번호로 바뀐다")
	void changesPassword() {
		authService.changePassword(emailUserId, CURRENT_PASSWORD, NEW_PASSWORD);
		entityManager.flush();
		entityManager.clear();

		String stored = userRepository.findById(emailUserId).orElseThrow().getPassword();

		assertThat(passwordEncoder.matches(NEW_PASSWORD, stored)).isTrue();
		assertThat(passwordEncoder.matches(CURRENT_PASSWORD, stored)).isFalse();
	}

	// 세션이 탈취된 상태에서 비밀번호까지 바꿀 수 있으면 계정을 통째로 빼앗긴다.
	@Test
	@DisplayName("현재 비밀번호가 틀리면 거부한다")
	void rejectsWrongCurrentPassword() {
		assertThatThrownBy(() -> authService.changePassword(emailUserId, "wrong!2026", NEW_PASSWORD))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.CURRENT_PASSWORD_MISMATCH);
	}

	@Test
	@DisplayName("비밀번호가 없는 소셜 전용 계정은 거부한다")
	void rejectsSocialOnlyAccount() {
		assertThatThrownBy(() -> authService.changePassword(socialOnlyUserId, CURRENT_PASSWORD, NEW_PASSWORD))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.PASSWORD_NOT_SET);
	}

	@Test
	@DisplayName("기존과 같은 비밀번호는 거부한다")
	void rejectsUnchangedPassword() {
		assertThatThrownBy(() -> authService.changePassword(emailUserId, CURRENT_PASSWORD, CURRENT_PASSWORD))
			.isInstanceOf(BaseException.class)
			.extracting(exception -> ((BaseException) exception).getErrorCode())
			.isEqualTo(AuthErrorCode.PASSWORD_UNCHANGED);
	}

}
