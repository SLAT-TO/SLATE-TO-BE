package com.slatto.global.config;

import com.slatto.global.config.properties.CorsProperties;
import com.slatto.global.security.CookieCsrfProtectionFilter;
import com.slatto.global.security.JwtAuthenticationEntryPoint;
import com.slatto.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CookieCsrfProtectionFilter cookieCsrfProtectionFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final CorsProperties corsProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			// 토큰 기반 무상태 API라 CSRF 토큰을 쓰지 않는다.
			// 대신 쿠키만으로 인증되는 경로는 CookieCsrfProtectionFilter가 Origin/Referer로 방어한다.
			.csrf(csrf -> csrf.disable())
			.formLogin(formLogin -> formLogin.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// /login/** 은 /api/v1/auth/login 자체도 매칭한다(** 는 0개 세그먼트도 받는다).
				// 이메일 로그인이 여기 묻히지 않도록 명시적으로 나열한다.
				.requestMatchers(
					"/api/v1/auth/login/**",
					"/api/v1/auth/callback/**",
					"/api/v1/auth/refresh",
					"/api/v1/auth/login",
					"/api/v1/auth/signup",
					"/api/v1/auth/password/reset",
					"/api/v1/auth/email/verification-codes",
					"/api/v1/auth/email/verification-codes/confirm"
				).permitAll()
					// 게스트 등록
					.requestMatchers(HttpMethod.POST, "/api/v1/share-links/*/guests").permitAll()

					// 게스트 피드백/답글 참여 (조회 포함 — 게스트 소유 검증은 서비스단에서 처리)
					.requestMatchers(HttpMethod.POST, "/api/v1/videos/*/feedbacks").permitAll()
					.requestMatchers(HttpMethod.GET,  "/api/v1/videos/*/feedbacks").permitAll()
					.requestMatchers(HttpMethod.PATCH, "/api/v1/feedbacks/*").permitAll()
					.requestMatchers(HttpMethod.DELETE, "/api/v1/feedbacks/*").permitAll()
					.requestMatchers(HttpMethod.POST, "/api/v1/feedbacks/*/replies").permitAll()
					.requestMatchers(HttpMethod.GET,  "/api/v1/feedbacks/*/replies").permitAll()
					.requestMatchers(HttpMethod.PATCH, "/api/v1/replies/*").permitAll()
					.requestMatchers(HttpMethod.DELETE, "/api/v1/replies/*").permitAll()

					.requestMatchers(
							HttpMethod.GET,
							"/api/v1/health",
							"/swagger-ui/**",
							"/v3/api-docs/**",
							"/api/v1/project-invitations/*",
							"/api/v1/share-links/*"   // 진입 검증 — 로그인 없이 접근
					).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(cookieCsrfProtectionFilter, JwtAuthenticationFilter.class)
			.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowCredentials(true);
		// PUT 은 프로필 이미지 업로드(PUT /api/v1/users/me/profile-image)가 쓴다.
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
