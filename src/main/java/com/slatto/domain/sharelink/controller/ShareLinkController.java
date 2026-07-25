package com.slatto.domain.sharelink.controller;

import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.ShareLinkCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkEntryResDTO;
import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.GuestCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkInfoResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkToggleResDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.slatto.domain.sharelink.service.ShareLinkService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "ShareLink", description = "공유 링크 API")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @Operation(summary = "공유 링크 생성", description = "영상당 1개만 생성 가능하며, 이미 있으면 409를 반환합니다.")
    @ResponseStatus(HttpStatus.CREATED)
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

    @Operation(summary = "공유 링크 진입 검증", description = "게스트가 링크로 접근했을 때 유효성을 확인합니다. 인증이 필요 없습니다.")
    @GetMapping("/share-links/{token}")
    public ApiResponse<ShareLinkEntryResDTO> getShareLinkByToken(
            @PathVariable String token
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                shareLinkService.getShareLinkByToken(token)
        );
    }

    @Operation(summary = "게스트 등록", description = "링크로 진입한 게스트가 이름을 등록하고 guestId를 발급받습니다. 인증이 필요 없습니다.")
    @ResponseStatus(HttpStatus.CREATED)
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

    @Operation(summary = "공유 링크 조회 (소유자용)", description = "영상의 공유 링크를 조회합니다. 프로젝트 멤버만 가능합니다.")
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