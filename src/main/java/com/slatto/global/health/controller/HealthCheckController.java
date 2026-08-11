package com.slatto.global.health.controller;

import com.slatto.global.health.dto.HealthCheckResponse;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
public class HealthCheckController {

	@Operation(
		summary = "서버 상태 확인",
		description = "서버가 요청을 받을 수 있는지만 확인한다. DB 등 외부 의존성 상태는 확인하지 않는다."
	)
	@SecurityRequirements
	@GetMapping("/api/v1/health")
	public ApiResponse<HealthCheckResponse> checkHealth() {
		return ApiResponse.success(CommonSuccessCode.OK, new HealthCheckResponse("OK"));
	}

}
