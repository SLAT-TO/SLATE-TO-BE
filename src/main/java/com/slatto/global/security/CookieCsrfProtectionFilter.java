package com.slatto.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.global.config.properties.CorsProperties;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 쿠키만으로 인증되는 엔드포인트의 CSRF 방어.
// CORS 허용 목록은 응답 읽기만 막고 Origin 헤더가 없는 요청은 CorsFilter를 그대로 통과한다.
// SameSite=None이면 크로스 사이트 요청에도 쿠키가 실리므로 신뢰 가능한 Origin(없으면 Referer)을 요구하고 판별 불가하면 거부한다.
@Slf4j
@Component
public class CookieCsrfProtectionFilter extends OncePerRequestFilter {

	private static final Set<String> PROTECTED_PATHS = Set.of("/api/v1/auth/refresh");

	private final Set<String> allowedOrigins;
	private final ObjectMapper objectMapper;

	public CookieCsrfProtectionFilter(CorsProperties corsProperties, ObjectMapper objectMapper) {
		this.allowedOrigins = normalizeAll(corsProperties.allowedOrigins());
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !HttpMethod.POST.matches(request.getMethod())
			|| !PROTECTED_PATHS.contains(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String origin = resolveOrigin(request);

		if (origin == null || !isTrusted(request, origin)) {
			log.warn("[CSRF] 신뢰할 수 없는 출처의 요청 차단. path={}, origin={}, referer={}",
				request.getRequestURI(), request.getHeader(HttpHeaders.ORIGIN), request.getHeader(HttpHeaders.REFERER));
			reject(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String resolveOrigin(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);

		if (origin != null && !origin.isBlank()) {
			return normalize(origin);
		}

		String referer = request.getHeader(HttpHeaders.REFERER);

		return referer == null || referer.isBlank() ? null : normalize(referer);
	}

	private boolean isTrusted(HttpServletRequest request, String origin) {
		return allowedOrigins.contains(origin) || origin.equals(requestOrigin(request));
	}

	// 프록시 뒤에서는 forward-headers-strategy: framework가 스킴·호스트·포트를 원본 요청 기준으로 되돌려준다.
	private String requestOrigin(HttpServletRequest request) {
		return toOrigin(request.getScheme(), request.getServerName(), request.getServerPort());
	}

	private static Set<String> normalizeAll(List<String> origins) {
		if (origins == null) {
			return Set.of();
		}

		Set<String> normalized = new LinkedHashSet<>();

		for (String origin : origins) {
			String value = normalize(origin);

			if (value != null) {
				normalized.add(value);
			}
		}

		return Set.copyOf(normalized);
	}

	private static String normalize(String value) {
		try {
			URI uri = URI.create(value.trim());

			if (uri.getScheme() == null || uri.getHost() == null) {
				return null;
			}

			return toOrigin(uri.getScheme(), uri.getHost(), uri.getPort());
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static String toOrigin(String scheme, String host, int port) {
		String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
		String normalizedHost = host.toLowerCase(Locale.ROOT);

		if (port <= 0 || port == defaultPort(normalizedScheme)) {
			return normalizedScheme + "://" + normalizedHost;
		}

		return normalizedScheme + "://" + normalizedHost + ":" + port;
	}

	private static int defaultPort(String scheme) {
		return switch (scheme) {
			case "https" -> 443;
			case "http" -> 80;
			default -> -1;
		};
	}

	private void reject(HttpServletResponse response) throws IOException {
		CommonErrorCode errorCode = CommonErrorCode.FORBIDDEN;

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode));
	}

}
