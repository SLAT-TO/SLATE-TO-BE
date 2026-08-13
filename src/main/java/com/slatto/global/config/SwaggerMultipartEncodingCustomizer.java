package com.slatto.global.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

/**
 * multipart 요청에서 JSON 본문으로 받는 파트에 encoding 을 붙인다.
 *
 * <p>springdoc 은 {@code @RequestPart} 로 받는 객체의 스키마만 만들고 encoding 은 비워 둔다.
 * 그러면 Swagger UI 가 그 파트를 Content-Type 없는 평문으로 실어 보내고, Spring 은 변환할
 * 컨버터를 찾지 못해 415 를 던진다. 문서에는 멀쩡히 보이는 엔드포인트가 Swagger 에서만 실패한다.
 *
 * <p>브라우저에서 직접 부르는 프론트는 Blob 에 Content-Type 을 직접 지정하고 있어 영향이 없다.
 * 즉 문서 쪽 정보가 빠진 것이지 API 동작의 문제가 아니라서, 컨트롤러 대신 여기서 채운다.
 */
@Component
public class SwaggerMultipartEncodingCustomizer implements OperationCustomizer {

	private static final String MULTIPART = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
	private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
	private static final String BINARY_FORMAT = "binary";
	private static final String OBJECT_TYPE = "object";
	private static final String ARRAY_TYPE = "array";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		RequestBody requestBody = operation.getRequestBody();

		if (requestBody == null || requestBody.getContent() == null) {
			return operation;
		}

		requestBody.getContent().forEach((mediaTypeName, mediaType) -> {
			if (mediaTypeName.startsWith(MULTIPART)) {
				applyJsonEncoding(mediaType);
			}
		});

		return operation;
	}

	private void applyJsonEncoding(MediaType mediaType) {
		Schema<?> schema = mediaType.getSchema();

		if (schema == null || schema.getProperties() == null) {
			return;
		}

		Map<String, Schema> properties = schema.getProperties();
		properties.forEach((name, propertySchema) -> {
			if (isJsonPart(propertySchema)) {
				mediaType.addEncoding(name, new Encoding().contentType(JSON));
			}
		});
	}

	// 파일 파트와 단순 값은 그대로 두고, 객체로 실려 가는 파트만 대상으로 한다.
	private boolean isJsonPart(Schema<?> propertySchema) {
		if (propertySchema == null || isBinary(propertySchema)) {
			return false;
		}

		return propertySchema.get$ref() != null
			|| OBJECT_TYPE.equals(propertySchema.getType())
			|| ARRAY_TYPE.equals(propertySchema.getType());
	}

	// 파일을 여러 개 받는 파트는 배열 자신이 아니라 items 에 format: binary 가 붙는다.
	// 배열이라는 이유로 JSON 취급하면 파일을 JSON 으로 실어 보내게 되어,
	// 이 클래스가 막으려던 것과 같은 실패를 반대 방향으로 만든다.
	private boolean isBinary(Schema<?> schema) {
		if (BINARY_FORMAT.equals(schema.getFormat())) {
			return true;
		}

		Schema<?> items = schema.getItems();

		return ARRAY_TYPE.equals(schema.getType()) && items != null && isBinary(items);
	}

}
