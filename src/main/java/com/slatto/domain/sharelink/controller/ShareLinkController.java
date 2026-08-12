package com.slatto.domain.sharelink.controller;

import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.ShareLinkCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkEntryResDTO;
import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.GuestCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkInfoResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkToggleResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.GuestVideoDetailResDTO;
import com.slatto.global.config.ApiErrorCodes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.slatto.domain.sharelink.service.ShareLinkService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
        name = "ShareLink",
        description = """
                공유 링크 API. 로그인하지 않은 게스트가 영상 한 편을 열람하고 피드백을 남기는 경로다.
                게스트 호출 순서는 다음과 같다.
                1. GET /share-links/{token} 으로 링크가 살아있는지 확인한다.
                2. POST /share-links/{token}/guests 로 이름을 등록하고 guestId 와 sessionToken 을 받는다.
                3. 이후 피드백·답글 요청에 sessionToken 을 X-Guest-Token 헤더로, guestId 를 파라미터로 함께 보낸다."""
)
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @Operation(summary = "공유 링크 생성", description = "영상당 1개만 생성 가능하며, 이미 있으면 409를 반환합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorCodes({"SHARELINK400", "PROJECT403", "SHARELINK409"})
    @PostMapping("/videos/{videoId}/share-links")
    public ApiResponse<ShareLinkCreateResDTO> createShareLink(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ShareLinkCreateReqDTO req
    ) {
        return ApiResponse.success(
                CommonSuccessCode.CREATED,
                shareLinkService.createShareLink(videoId, userId, req)
        );
    }

    @Operation(
            summary = "공유 링크 진입 검증",
            description = """
                    게스트가 링크로 접근했을 때 유효성을 확인합니다. 인증이 필요 없습니다.
                    토큰이 URL 경로에 드러나는 것은 의도된 설계입니다. 링크를 아는 것 자체가 이 영상 한 편에 대한 열람 자격이기 때문에,
                    별도 자격 증명을 요구하지 않습니다. 토큰은 UUID 이고 영상 하나에만 연결되며,
                    소유자가 비활성화하거나 만료되면 SHARELINK410 으로 즉시 막힙니다."""
    )
    @SecurityRequirements
    @ApiErrorCodes({"SHARELINK404", "SHARELINK410"})
    @GetMapping("/share-links/{token}")
    public ApiResponse<ShareLinkEntryResDTO> getShareLinkByToken(
            @PathVariable String token
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                shareLinkService.getShareLinkByToken(token)
        );
    }

    @Operation(
            summary = "게스트 등록",
            description = """
                    링크로 진입한 게스트가 이름을 등록하고 guestId 와 sessionToken 을 발급받습니다. 인증이 필요 없습니다.
                    응답의 sessionToken 이 이후 게스트 요청의 X-Guest-Token 헤더 값이고, guestId 는 같은 요청의 guestId 파라미터 값입니다.
                    서버에는 해시만 저장하므로 sessionToken 원문은 이 응답에서만 확인할 수 있습니다."""
    )
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @ApiErrorCodes({"SHARELINK404", "SHARELINK410"})
    @PostMapping("/share-links/{token}/guests")
    public ApiResponse<GuestCreateResDTO> registerGuest(
            @PathVariable String token,
            @RequestBody @Valid GuestCreateReqDTO req
    ) {
        return ApiResponse.success(
                CommonSuccessCode.CREATED,
                shareLinkService.registerGuest(token, req)
        );
    }

    @Operation(
            summary = "게스트 영상 상세 조회",
            description = "공유 링크를 통해 등록된 게스트가 영상과 프로젝트 정보를 읽기 전용으로 조회합니다."
    )
    @SecurityRequirements
    @ApiErrorCodes({"SHARELINK404", "SHARELINK410", "SHARELINK403", "COMMON404"})
    @GetMapping("/share-links/{shareToken}/video")
    public ApiResponse<GuestVideoDetailResDTO> getGuestVideo(
            @PathVariable String shareToken,
            @RequestHeader("X-Guest-Id") Long guestId,
            @RequestHeader("X-Guest-Token") String guestToken
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                shareLinkService.getGuestVideo(shareToken, guestId, guestToken)
        );
    }

    @Operation(summary = "공유 링크 조회 (소유자용)", description = "영상의 공유 링크를 조회합니다. 프로젝트 멤버만 가능합니다.")
    @ApiErrorCodes({"PROJECT403", "SHARELINK404"})
    @GetMapping("/videos/{videoId}/share-links")
    public ApiResponse<
            ShareLinkInfoResDTO> getShareLinkByVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                shareLinkService.getShareLinkByVideo(videoId, userId)
        );
    }

    @Operation(summary = "공유 링크 활성/비활성 토글", description = "공유 링크의 활성 상태를 뒤집습니다. 프로젝트 멤버만 가능합니다.")
    @ApiErrorCodes({"PROJECT403", "SHARELINK404"})
    @PatchMapping("/share-links/{shareLinkId}")
    public ApiResponse<ShareLinkToggleResDTO> toggleShareLink(
            @PathVariable Long shareLinkId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                shareLinkService.toggleShareLink(shareLinkId, userId)
        );
    }

}
