package com.slatto.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("expires_in") Long expiresIn,
	@JsonProperty("token_type") String tokenType
) {
}
