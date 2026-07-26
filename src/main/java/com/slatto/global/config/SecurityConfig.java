package com.slatto.global.config;

import com.slatto.global.security.JwtAuthenticationEntryPoint;
import com.slatto.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/v1/auth/login/**",
								"/api/v1/auth/callback/**",
								"/api/v1/auth/refresh"
						).permitAll()
						// 게스트 등록 — 로그인 없이 접근 (게스트 접근 기능)
						.requestMatchers(HttpMethod.POST, "/api/v1/share-links/*/guests").permitAll()

						// 게스트 피드백/답글 참여 — 로그인 없이 접근 (해결 상태 변경은 제외 → 멤버 전용)
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
				.build();
	}
}