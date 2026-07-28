package com.slatto.global.config.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookiePropertiesTest {

	@Test
	@DisplayName("SameSite=None이면서 secure=false면 생성에 실패한다")
	void rejectsSameSiteNoneWithoutSecure() {
		assertThatThrownBy(() -> create("None", false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("app.cookie.same-site");
	}

	@Test
	@DisplayName("SameSite=none 소문자 표기도 동일하게 막는다")
	void rejectsLowerCaseSameSiteNoneWithoutSecure() {
		assertThatThrownBy(() -> create("none", false))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("SameSite=None이고 secure=true면 정상 생성된다")
	void allowsSameSiteNoneWithSecure() {
		assertThatCode(() -> create("None", true)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("SameSite=Lax는 secure=false여도 허용한다")
	void allowsLaxWithoutSecure() {
		assertThatCode(() -> create("Lax", false)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("지원하지 않는 SameSite 값은 거부한다")
	void rejectsUnknownSameSite() {
		assertThatThrownBy(() -> create("Nono", true))
			.isInstanceOf(IllegalStateException.class);
	}

	private CookieProperties create(String sameSite, boolean secure) {
		return new CookieProperties(
			"refreshToken",
			"oauthState",
			"/api/v1/auth",
			secure,
			sameSite,
			"Lax",
			Duration.ofMinutes(5)
		);
	}

}
