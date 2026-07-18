package com.slatto.domain.auth.service;

import com.slatto.domain.auth.client.GoogleOAuthClient;
import com.slatto.domain.auth.support.OAuthState;
import com.slatto.global.config.properties.FrontendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final GoogleOAuthClient googleOAuthClient;
	private final FrontendProperties frontendProperties;

	public GoogleLoginEntry createGoogleLoginEntry(String redirectTo) {
		OAuthState state = OAuthState.create(frontendProperties.resolveRedirectPath(redirectTo));

		return new GoogleLoginEntry(googleOAuthClient.buildAuthorizationUri(state.value()), state);
	}

	public record GoogleLoginEntry(String authorizationUri, OAuthState state) {
	}

}
