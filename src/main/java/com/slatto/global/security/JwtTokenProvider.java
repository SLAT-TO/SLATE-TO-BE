package com.slatto.global.security;

import com.slatto.global.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

	private static final String CLAIM_TOKEN_TYPE = "type";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(Long userId) {
		return createToken(userId, TOKEN_TYPE_ACCESS, jwtProperties.accessTokenValidity().toMillis());
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, TOKEN_TYPE_REFRESH, jwtProperties.refreshTokenValidity().toMillis());
	}

	private String createToken(Long userId, String tokenType, long validityInMillis) {
		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + validityInMillis);

		return Jwts.builder()
			.issuer(jwtProperties.issuer())
			.subject(String.valueOf(userId))
			.claim(CLAIM_TOKEN_TYPE, tokenType)
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();
	}

	public Long parseUserId(String token, boolean refreshToken) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.requireIssuer(jwtProperties.issuer())
				.require(CLAIM_TOKEN_TYPE, refreshToken ? TOKEN_TYPE_REFRESH : TOKEN_TYPE_ACCESS)
				.build()
				.parseSignedClaims(token)
				.getPayload();

			return Long.valueOf(claims.getSubject());
		} catch (JwtException | IllegalArgumentException exception) {
			log.debug("[JWT] 토큰 검증 실패: {}", exception.getMessage());
			return null;
		}
	}

	public LocalDateTime refreshTokenExpiresAt() {
		return LocalDateTime.ofInstant(
			Instant.now().plusMillis(jwtProperties.refreshTokenValidity().toMillis()),
			ZoneId.systemDefault()
		);
	}

	public long refreshTokenMaxAgeSeconds() {
		return jwtProperties.refreshTokenValidity().toSeconds();
	}

}
