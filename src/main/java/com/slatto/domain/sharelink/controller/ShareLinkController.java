package com.slatto.domain.sharelink.controller;

import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.ShareLinkCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
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
@Tag(name = "공유 링크 API")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @Operation(summary = "공유 링크 생성", description = "영상당 1개만 생성 가능하며, 이미 있으면 409를 반환합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/videos/{videoId}/share-links")
    public ApiResponse<ShareLinkCreateResDTO> createShareLink(
            @PathVariable Long videoId,
            @RequestBody @Valid ShareLinkCreateReqDTO req
    ) {
        return ApiResponse.success(
                CommonSuccessCode.CREATED,
                shareLinkService.createShareLink(videoId, req)
        );
    }
}