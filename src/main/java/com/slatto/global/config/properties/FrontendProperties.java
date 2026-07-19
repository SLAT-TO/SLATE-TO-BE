package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
	String baseUrl,
	String callbackPath,
	String errorPath,
	List<String> allowedRedirectPaths
) {

	public String resolveRedirectPath(String redirectTo) {
		if (redirectTo != null && allowedRedirectPaths.contains(redirectTo)) {
			return redirectTo;
		}

		return callbackPath;
	}

	public String toAbsoluteUrl(String path) {
		return baseUrl + path;
	}

}
