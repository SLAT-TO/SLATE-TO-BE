package com.slatto.global.security;

import com.slatto.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

	private static final Long USER_ID = 42L;
	private static final String TOKEN = "valid-access-token";

	private JwtTokenProvider jwtTokenProvider;
	private UserRepository userRepository;
	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = mock(JwtTokenProvider.class);
		userRepository = mock(UserRepository.class);
		filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("활성 유저의 토큰이면 인증을 세운다")
	void authenticatesActiveUser() throws Exception {
		given(jwtTokenProvider.parseUserId(eq(TOKEN), eq(false))).willReturn(USER_ID);
		given(userRepository.existsByIdAndDeletedAtIsNull(USER_ID)).willReturn(true);

		doFilterWithToken();

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(authentication.getPrincipal()).isEqualTo(USER_ID);
	}

	// 탈퇴해도 이미 발급된 토큰은 만료까지 서명 검증을 통과한다.
	// 여기서 걸러내지 않으면 탈퇴 후에도 그 시간 동안 API 가 열린다.
	@Test
	@DisplayName("탈퇴한 유저의 토큰이면 인증을 세우지 않는다")
	void rejectsWithdrawnUser() throws Exception {
		given(jwtTokenProvider.parseUserId(eq(TOKEN), eq(false))).willReturn(USER_ID);
		given(userRepository.existsByIdAndDeletedAtIsNull(USER_ID)).willReturn(false);

		doFilterWithToken();

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("토큰이 유효하지 않으면 유저를 조회하지 않는다")
	void skipsUserLookupForInvalidToken() throws Exception {
		given(jwtTokenProvider.parseUserId(anyString(), eq(false))).willReturn(null);

		doFilterWithToken();

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userRepository, never()).existsByIdAndDeletedAtIsNull(anyLong());
	}

	@Test
	@DisplayName("Authorization 헤더가 없으면 유저를 조회하지 않는다")
	void skipsUserLookupWithoutHeader() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userRepository, never()).existsByIdAndDeletedAtIsNull(anyLong());
	}

	private void doFilterWithToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
	}

}
