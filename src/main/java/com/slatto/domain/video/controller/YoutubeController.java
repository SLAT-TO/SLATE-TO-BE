package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.request.VideoRequest.YoutubeValidateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.YoutubeValidateResDTO;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/videos/youtube")
public class YoutubeController {

    private final VideoService videoService;

    @PostMapping("/validate")
    @Operation(summary = "YouTube URL 검증", description = "YouTube 영상 정보와 프로젝트 내 중복 등록 여부를 검증합니다.")
    public ApiResponse<YoutubeValidateResDTO> validateYoutubeUrl(
            @Valid @RequestBody YoutubeValidateReqDTO request
    ) {
        // TODO: 인증/인가 구현 후 JWT에서 memberId 추출하도록 변경
        Long memberId = 1L;
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.validateYoutubeUrl(memberId, request)
        );
    }
}
