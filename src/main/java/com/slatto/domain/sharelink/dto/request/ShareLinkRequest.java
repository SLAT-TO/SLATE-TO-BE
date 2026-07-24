package com.slatto.domain.sharelink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ShareLinkRequest {

    @Schema(description = "공유 링크 생성 요청")
    public record ShareLinkCreateReqDTO(
            @Schema(example = "1")
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @Schema(description = "만료 일시. 미지정 시 무기한", example = "2026-12-31T23:59:59", nullable = true)
            LocalDateTime expiredAt
    ) { }
}