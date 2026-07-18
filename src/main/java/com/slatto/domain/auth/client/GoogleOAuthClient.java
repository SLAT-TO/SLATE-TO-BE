package com.slatto.domain.auth.client;

import com.slatto.domain.auth.client.dto.GoogleTokenResponse;
import com.slatto.domain.auth.client.dto.GoogleUserInfo;
import com.slatto.global.config.properties.GoogleOAuthProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthClient {

	private final GoogleOAuthProperties googleOAuthProperties;
	private final RestClient restClient;

	public GoogleOAuthClient(GoogleOAuthProperties googleOAuthProperties, RestClient.Builder restClientBuilder) {
		this.googleOAuthProperties = googleOAuthProperties;
		this.restClient = restClientBuilder.build();
	}

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

	public GoogleTokenResponse exchangeCodeForToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", code);
		form.add("client_id", googleOAuthProperties.clientId());
		form.add("client_secret", googleOAuthProperties.clientSecret());
		form.add("redirect_uri", googleOAuthProperties.redirectUri());
		form.add("grant_type", "authorization_code");

		return restClient.post()
			.uri(googleOAuthProperties.tokenUri())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(GoogleTokenResponse.class);
	}

	public GoogleUserInfo fetchUserInfo(String accessToken) {
		return restClient.get()
			.uri(googleOAuthProperties.userInfoUri())
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(GoogleUserInfo.class);
	}

}
