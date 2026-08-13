package com.slatto.global.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 어떤 multipart 파트에 application/json 을 붙일지 고정한다.
 *
 * <p>파트마다 판단이 갈린다. 객체는 붙여야 Swagger UI 가 415 를 내지 않고, 파일은 붙이면
 * 파일을 JSON 으로 실어 보내 같은 실패가 반대로 난다. 배열은 겉모습만으로는 둘을 구분할 수
 * 없고 items 를 봐야 한다.
 *
 * <p>실제 스펙을 훑는 {@code OpenApiDocumentationTest} 로는 이 구분을 검증할 수 없다.
 * 지금 프로젝트에 배열 파트를 받는 엔드포인트가 없어서 훑을 대상 자체가 없기 때문에,
 * 여기서 스키마를 직접 만들어 넣는다.
 */
class SwaggerMultipartEncodingCustomizerTest {

	private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
	private static final String MULTIPART = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

	private final SwaggerMultipartEncodingCustomizer customizer = new SwaggerMultipartEncodingCustomizer();

	@Test
	@DisplayName("객체 파트에는 application/json 을 붙인다")
	void addsJsonEncodingToObjectPart() {
		MediaType mediaType = customizeMultipart("request", new ObjectSchema());

		assertThat(contentTypeOf(mediaType, "request")).isEqualTo(JSON);
	}

	@Test
	@DisplayName("$ref 로 실린 파트에도 application/json 을 붙인다")
	void addsJsonEncodingToReferencedPart() {
		MediaType mediaType = customizeMultipart("request", new Schema<>().$ref("#/components/schemas/UploadRequest"));

		assertThat(contentTypeOf(mediaType, "request")).isEqualTo(JSON);
	}

	@Test
	@DisplayName("객체 배열 파트에도 application/json 을 붙인다")
	void addsJsonEncodingToJsonArrayPart() {
		MediaType mediaType = customizeMultipart("items", new ArraySchema().items(new ObjectSchema()));

		assertThat(contentTypeOf(mediaType, "items")).isEqualTo(JSON);
	}

	@Test
	@DisplayName("파일 파트는 건드리지 않는다")
	void leavesBinaryPartAlone() {
		MediaType mediaType = customizeMultipart("file", new StringSchema().format("binary"));

		assertThat(mediaType.getEncoding()).isNull();
	}

	// 배열 자신에는 format 이 없고 items 에만 binary 가 붙는다. 배열이라는 이유로 JSON 을
	// 붙이면 Swagger UI 가 파일을 JSON 으로 실어 보낸다.
	@Test
	@DisplayName("파일 배열 파트는 건드리지 않는다")
	void leavesBinaryArrayPartAlone() {
		MediaType mediaType = customizeMultipart("files", new ArraySchema().items(new StringSchema().format("binary")));

		assertThat(mediaType.getEncoding()).isNull();
	}

	@Test
	@DisplayName("단순 값 파트는 건드리지 않는다")
	void leavesSimpleValuePartAlone() {
		MediaType mediaType = customizeMultipart("fileName", new StringSchema());

		assertThat(mediaType.getEncoding()).isNull();
	}

	// multipart 가 아닌 본문은 파트라는 개념이 없어 encoding 이 의미가 없다.
	@Test
	@DisplayName("multipart 가 아닌 본문은 건드리지 않는다")
	void leavesNonMultipartBodyAlone() {
		MediaType mediaType = new MediaType().schema(new ObjectSchema().addProperty("request", new ObjectSchema()));
		Operation operation = new Operation()
			.requestBody(new RequestBody().content(new Content().addMediaType(JSON, mediaType)));

		customizer.customize(operation, null);

		assertThat(mediaType.getEncoding()).isNull();
	}

	@Test
	@DisplayName("본문이 없는 오퍼레이션도 그대로 통과한다")
	void ignoresOperationWithoutRequestBody() {
		Operation operation = new Operation();

		assertThat(customizer.customize(operation, null)).isSameAs(operation);
	}

	private MediaType customizeMultipart(String partName, Schema<?> partSchema) {
		MediaType mediaType = new MediaType()
			.schema(new ObjectSchema().addProperty(partName, partSchema));
		Operation operation = new Operation()
			.requestBody(new RequestBody().content(new Content().addMediaType(MULTIPART, mediaType)));

		customizer.customize(operation, null);

		return mediaType;
	}

	private String contentTypeOf(MediaType mediaType, String partName) {
		return mediaType.getEncoding().get(partName).getContentType();
	}

}
