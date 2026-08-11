package com.slatto.global.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * multipart 한도를 넘긴 업로드가 공통 응답 포맷으로 나가는지 검증한다.
 *
 * <p>한도 초과는 요청 본문을 읽는 단계에서 터져 컨트롤러에 닿지 않는다.
 * 그래서 서비스의 파일 크기 검증은 실행되지 않고, 전역 핸들러가 잡지 않으면 COMMON500 이 나간다.
 *
 * <p>MockMvc 는 multipart 요청을 테스트가 직접 조립하기 때문에 파싱 자체가 일어나지 않는다.
 * 한도는 서블릿 컨테이너가 강제하므로 실제 포트를 띄워 진짜 HTTP 요청을 보낸다.
 */
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = {
		"spring.servlet.multipart.max-file-size=1KB",
		"spring.servlet.multipart.max-request-size=10MB"
	}
)
class MultipartUploadLimitTest {

	// 인증 필터에서 먼저 걸리지 않도록 permitAll 경로를 쓴다.
	// multipart 파싱은 핸들러를 찾기 전에 일어나므로 이 경로가 파일을 받는지는 상관없다.
	private static final String PERMIT_ALL_PATH = "/api/v1/videos/1/feedbacks";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("업로드 한도를 넘긴 multipart 요청은 500 이 아니라 413 COMMON413 으로 응답한다")
	void respondsWithPayloadTooLargeWhenUploadLimitExceeded() throws Exception {
		ResponseEntity<String> response = upload(oversizedFile());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

		JsonNode body = objectMapper.readTree(response.getBody());

		assertThat(body.get("isSuccess").asBoolean()).isFalse();
		assertThat(body.get("code").asText()).isEqualTo("COMMON413");
		assertThat(body.get("message").asText()).isNotBlank();
	}

	private ResponseEntity<String> upload(ByteArrayResource file) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", file);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		return restTemplate.postForEntity(PERMIT_ALL_PATH, new HttpEntity<>(body, headers), String.class);
	}

	private ByteArrayResource oversizedFile() {
		return new ByteArrayResource(new byte[4096]) {
			@Override
			public String getFilename() {
				return "oversized.bin";
			}
		};
	}

}
