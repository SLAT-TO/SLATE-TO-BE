package com.slatto.domain.auth.client;

import com.slatto.global.config.properties.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

	private final GoogleOAuthProperties googleOAuthProperties;

	public String buildAuthorizationUri(String state) {
		return UriComponentsBuilder.fromUriString(googleOAuthProperties.authorizationUri())
			.queryParam("client_id", googleOAuthProperties.clientId())
			.queryParam("redirect_uri", googleOAuthProperties.redirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", googleOAuthProperties.scope())
			.queryParam("state", state)
			.queryParam("access_type", "offline")
			.build()
			.encode()
			.toUriString();
	}

}
