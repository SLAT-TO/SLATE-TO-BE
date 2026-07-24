package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.request.VideoRequest.YoutubeValidateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.YoutubeValidateResDTO;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "YouTube", description = "YouTube 영상 URL 및 등록 가능 여부 검증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos/youtube")
public class YoutubeController {

    private final VideoService videoService;

    @PostMapping("/validate")
    @Operation(
            summary = "YouTube URL 검증",
            description = "프로젝트 멤버가 등록 전에 YouTube URL을 검증합니다. URL에서 영상 ID를 추출한 뒤 " +
                    "프로젝트 내 중복 여부와 YouTube의 공개·재생 가능 상태를 확인하고 영상 정보를 반환합니다. " +
                    "이 API는 영상을 저장하지 않습니다."
    )
    public ApiResponse<YoutubeValidateResDTO> validateYoutubeUrl(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody YoutubeValidateReqDTO request
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.validateYoutubeUrl(memberId, request)
        );
    }
}
