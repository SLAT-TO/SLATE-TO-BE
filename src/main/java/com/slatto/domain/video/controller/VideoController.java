package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.response.VideoResponse.VideoListResDTO;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/videos")
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    @Operation(summary = "영상 목록 조회", description = "프로젝트 멤버가 프로젝트에 등록된 영상을 커서 방식으로 조회합니다.")
    public ApiResponse<VideoListResDTO> getVideos(
            @PathVariable @Positive Long projectId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size
    ) {
        // TODO: 인증/인가 구현 후 JWT에서 memberId 추출하도록 변경
        Long memberId = 1L;
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.getVideos(memberId, projectId, cursor, size)
        );
    }
}
