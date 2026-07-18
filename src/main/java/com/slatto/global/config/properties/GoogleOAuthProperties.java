package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.google")
public record GoogleOAuthProperties(
	String clientId,
	String clientSecret,
	String redirectUri,
	String authorizationUri,
	String tokenUri,
	String userInfoUri,
	String scope
) {
}
