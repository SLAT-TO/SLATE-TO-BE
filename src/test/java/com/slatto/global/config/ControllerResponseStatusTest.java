package com.slatto.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * springdoc 은 핸들러의 {@code @ResponseStatus} 만 읽는다.
 * {@code ResponseEntity} 로 지정한 상태는 문서에 반영되지 않아, 실제로 201·302 를 반환하면서
 * 문서에는 200 이 실린다. 컴파일도 호출도 성공하므로 문서를 열어보기 전에는 드러나지 않는다.
 *
 * <p>생성된 문서로는 잡을 수 없다. 문서의 성공 상태 코드 자체가 {@code @ResponseStatus} 에서
 * 나오기 때문에, 둘을 비교하면 같은 값을 두 번 읽고 항상 통과한다. 그래서 소스를 직접 읽는다.
 */
class ControllerResponseStatusTest {

	private static final Path SOURCE_ROOT = Path.of("src/main/java");

	private static final Pattern MAPPING =
		Pattern.compile("^\\s*@(Get|Post|Put|Patch|Delete|Request)Mapping\\b");

	@Test
	@DisplayName("ResponseEntity 로 상태를 지정한 핸들러는 @ResponseStatus 도 함께 표기한다")
	void handlersSettingStatusExplicitlyDeclareResponseStatus() throws IOException {
		List<String> checked = new ArrayList<>();
		List<String> missing = new ArrayList<>();

		for (Path controller : controllerSources()) {
			List<String> lines = Files.readAllLines(controller);

			for (int line = 0; line < lines.size(); line++) {
				if (!lines.get(line).contains(".status(")) {
					continue;
				}

				int mapping = previousMapping(lines, line);

				if (mapping < 0) {
					continue;
				}

				String annotations = String.join("\n", lines.subList(previousMapping(lines, mapping - 1) + 1, mapping));

				// 문서에 노출되지 않는 핸들러는 어긋날 문서가 없다.
				if (annotations.contains("@Hidden")) {
					continue;
				}

				String location = controller.getFileName() + ":" + (line + 1);
				checked.add(location);

				if (!annotations.contains("@ResponseStatus")) {
					missing.add(location);
				}
			}
		}

		assertThat(checked)
			.as("ResponseEntity 로 상태를 직접 지정하는 핸들러")
			.isNotEmpty();
		assertThat(missing)
			.as("상태를 직접 지정했지만 @ResponseStatus 가 없어 문서에는 200 으로 실리는 핸들러")
			.isEmpty();
	}

	private List<Path> controllerSources() throws IOException {
		try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
			return paths
				.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
				.toList();
		}
	}

	// 애노테이션 블록은 바로 앞 매핑과 이 매핑 사이에 있다.
	// 빈 줄을 경계로 삼으면 @Operation 의 텍스트 블록 안 빈 줄에서 잘린다.
	private int previousMapping(List<String> lines, int from) {
		for (int line = from; line >= 0; line--) {
			if (MAPPING.matcher(lines.get(line)).find()) {
				return line;
			}
		}

		return -1;
	}

}
