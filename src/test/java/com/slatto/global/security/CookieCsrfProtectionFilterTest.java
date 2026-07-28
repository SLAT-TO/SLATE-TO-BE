package com.slatto.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.global.config.properties.CorsProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookieCsrfProtectionFilterTest {

	private static final String REFRESH_URI = "/api/v1/auth/refresh";

	private CookieCsrfProtectionFilter filter;

	@BeforeEach
	void setUp() {
		filter = new CookieCsrfProtectionFilter(
			new CorsProperties(List.of("https://slatto.site", "http://localhost:3000")),
			new ObjectMapper()
		);
	}

	@Test
	@DisplayName("허용된 오리진의 재발급 요청은 통과한다")
	void allowsRequestFromAllowedOrigin() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.addHeader(HttpHeaders.ORIGIN, "https://slatto.site");

		assertThat(doFilter(request).getStatus()).isEqualTo(200);
	}

	@Test
	@DisplayName("기본 포트가 생략된 오리진도 동일하게 취급한다")
	void allowsRequestFromAllowedOriginWithDefaultPort() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.addHeader(HttpHeaders.ORIGIN, "https://SLATTO.site:443");

		assertThat(doFilter(request).getStatus()).isEqualTo(200);
	}

	@Test
	@DisplayName("허용 목록에 없는 오리진의 재발급 요청은 403으로 차단한다")
	void blocksRequestFromDisallowedOrigin() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.addHeader(HttpHeaders.ORIGIN, "https://evil.example.com");

		MockHttpServletResponse response = doFilter(request);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("COMMON403");
	}

	@Test
	@DisplayName("Origin이 없으면 Referer의 오리진으로 판단한다")
	void fallsBackToReferer() throws Exception {
		MockHttpServletRequest allowed = refreshRequest();
		allowed.addHeader(HttpHeaders.REFERER, "https://slatto.site/projects/1?tab=schedule");

		MockHttpServletRequest disallowed = refreshRequest();
		disallowed.addHeader(HttpHeaders.REFERER, "https://evil.example.com/attack.html");

		assertThat(doFilter(allowed).getStatus()).isEqualTo(200);
		assertThat(doFilter(disallowed).getStatus()).isEqualTo(403);
	}

	@Test
	@DisplayName("Origin과 Referer가 모두 없으면 차단한다")
	void blocksRequestWithoutOriginAndReferer() throws Exception {
		assertThat(doFilter(refreshRequest()).getStatus()).isEqualTo(403);
	}

	@Test
	@DisplayName("리다이렉트 등으로 오리진이 null이면 차단한다")
	void blocksNullOrigin() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.addHeader(HttpHeaders.ORIGIN, "null");

		assertThat(doFilter(request).getStatus()).isEqualTo(403);
	}

	@Test
	@DisplayName("허용 목록에 없어도 API 자신과 동일 오리진이면 통과한다")
	void allowsSameOriginRequest() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.setScheme("https");
		request.setServerName("api.slatto.site");
		request.setServerPort(443);
		request.addHeader(HttpHeaders.ORIGIN, "https://api.slatto.site");

		assertThat(doFilter(request).getStatus()).isEqualTo(200);
	}

	@Test
	@DisplayName("퍼센트 인코딩으로 경로를 우회할 수 없다")
	void blocksPercentEncodedPathBypass() throws Exception {
		MockHttpServletRequest request = refreshRequest();
		request.setRequestURI("/api/v1/auth/%72efresh");
		request.addHeader(HttpHeaders.ORIGIN, "https://evil.example.com");

		assertThat(doFilter(request).getStatus()).isEqualTo(403);
	}

	@Test
	@DisplayName("보호 대상이 아닌 경로는 오리진과 무관하게 통과한다")
	void ignoresUnprotectedPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects");
		request.addHeader(HttpHeaders.ORIGIN, "https://evil.example.com");

		assertThat(doFilter(request).getStatus()).isEqualTo(200);
	}

	private MockHttpServletRequest refreshRequest() {
		return new MockHttpServletRequest("POST", REFRESH_URI);
	}

	private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		return response;
	}

}
