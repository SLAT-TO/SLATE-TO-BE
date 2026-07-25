package com.slatto.domain.sharelink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ShareLinkRequest {

    @Schema(description = "공유 링크 생성 요청")
    public record ShareLinkCreateReqDTO(
            @Schema(description = "만료 일시. 미지정 시 무기한", example = "2026-12-31T23:59:59", nullable = true)
            LocalDateTime expiredAt
    ) { }

    @Schema(description = "게스트 등록 요청")
    public record GuestCreateReqDTO(
            @Schema(example = "홍길동")
            @NotBlank(message = "이름은 필수입니다.")
            @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
            String name
    ) { }
}