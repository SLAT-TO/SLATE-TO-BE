package com.slatto.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailSenderProperties(
	String fromAddress,
	String fromName
) {
}
