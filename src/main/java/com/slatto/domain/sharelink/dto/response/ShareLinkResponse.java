package com.slatto.domain.sharelink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class ShareLinkResponse {

    @Schema(description = "공유 링크 생성 응답")
    public record ShareLinkCreateResDTO(
            @Schema(example = "5")
            Long shareLinkId,

            @Schema(example = "10")
            Long videoId,

            @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
            String token,

            @Schema(example = "true")
            Boolean isActive,

            @Schema(example = "2026-12-31T23:59:59", nullable = true)
            LocalDateTime expiredAt,

            @Schema(example = "2026-07-24T14:00:00")
            LocalDateTime createdAt
    ) { }

    @Schema(description = "공유 링크 진입 검증 응답")
    public record ShareLinkEntryResDTO(
            @Schema(example = "10")
            Long videoId,

            @Schema(example = "1차 편집본")
            String videoTitle,

            @Schema(example = "true", description = "게스트 닉네임 입력이 필요한지 여부")
            Boolean requiresNickname
    ) { }

    @Schema(description = "게스트 등록 응답")
    public record GuestCreateResDTO(
            @Schema(example = "20")
            Long guestId,

            @Schema(example = "5")
            Long shareLinkId,

            @Schema(example = "홍길동")
            String name,

            @Schema(example = "2026-07-24T14:00:00")
            LocalDateTime createdAt
    ) { }

    @Schema(description = "공유 링크 조회 응답 (소유자용)")
    public record ShareLinkInfoResDTO(
            @Schema(example = "5")
            Long shareLinkId,

            @Schema(example = "10")
            Long videoId,

            @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
            String token,

            @Schema(example = "true")
            Boolean isActive,

            @Schema(example = "2026-12-31T23:59:59", nullable = true)
            LocalDateTime expiredAt,

            @Schema(example = "2026-07-24T14:00:00")
            LocalDateTime createdAt
    ) { }
}