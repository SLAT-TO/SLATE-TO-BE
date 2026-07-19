package com.slatto.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfo(
	String sub,
	String email,
	@JsonProperty("email_verified") Boolean emailVerified,
	String name,
	String picture
) {

	public boolean isEmailVerified() {
		return Boolean.TRUE.equals(emailVerified);
	}

}
