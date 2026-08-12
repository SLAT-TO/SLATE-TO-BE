package com.slatto.global.response.code;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 코드 문자열로 {@link BaseCode} 를 되찾는다.
 *
 * <p>애노테이션 배열에는 한 가지 타입만 담을 수 있어서
 * {@code @ApiErrorCodes({ProjectErrorCode.X, CommonErrorCode.Y})} 같은 표기는 컴파일되지 않는다.
 * 그래서 애노테이션은 코드 문자열을 받고, 실제 enum 은 여기서 찾는다.
 *
 * <p>문자열이라 오타를 컴파일러가 잡아주지 못한다.
 * 대신 문서 검증 테스트가 모든 표기를 이 레지스트리로 해석해보고 실패시킨다.
 */
@Component
public class ErrorCodeRegistry {

	private static final String BASE_PACKAGE = "com.slatto";

	private final Map<String, BaseCode> codes;

	public ErrorCodeRegistry() {
		this.codes = scanErrorCodes();
	}

	public BaseCode find(String code) {
		BaseCode found = codes.get(code);

		if (found == null) {
			throw new IllegalArgumentException("존재하지 않는 에러 코드입니다: " + code);
		}

		return found;
	}

	public boolean contains(String code) {
		return codes.containsKey(code);
	}

	// enum 을 손으로 등록하면 새 도메인을 추가할 때 빠뜨려도 아무 신호가 없다.
	// BaseCode 구현체를 훑어서 자동으로 채운다.
	private Map<String, BaseCode> scanErrorCodes() {
		ClassPathScanningCandidateComponentProvider scanner =
			new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(BaseCode.class));

		Map<String, BaseCode> found = new LinkedHashMap<>();

		for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
			Class<?> type = resolve(definition.getBeanClassName());

			if (!type.isEnum()) {
				continue;
			}

			for (Object constant : type.getEnumConstants()) {
				register(found, (BaseCode) constant);
			}
		}

		return found;
	}

	// 코드 문자열이 겹치면 어느 쪽이 문서에 실릴지 정할 수 없다.
	// 조용히 덮어쓰는 대신 기동 시점에 깨뜨린다.
	private void register(Map<String, BaseCode> found, BaseCode code) {
		if (code.isSuccess()) {
			return;
		}

		BaseCode previous = found.put(code.getCode(), code);

		if (previous != null && previous != code) {
			throw new IllegalStateException(
				"에러 코드 문자열이 중복됩니다: " + code.getCode()
					+ " (" + previous.getClass().getSimpleName() + ", " + code.getClass().getSimpleName() + ")"
			);
		}
	}

	private Class<?> resolve(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException("에러 코드 클래스를 읽을 수 없습니다: " + className, exception);
		}
	}
}
