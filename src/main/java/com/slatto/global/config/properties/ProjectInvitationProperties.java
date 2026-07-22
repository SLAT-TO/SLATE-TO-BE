package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invitation")
public record ProjectInvitationProperties(
    String baseUrl
) {
}
