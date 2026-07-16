package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.request.VideoRequest.VideoCreateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoUpdateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoListResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoUpdateResDTO;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/videos")
public class VideoController {

    private final VideoService videoService;

    @PatchMapping("/{videoId}")
    @Operation(summary = "영상 수정",
            description = "프로젝트에 등록된 영상의 제목과 메모를 수정합니다.<br>" +
                    "수정하지 않을 부분은 null로 넣으면 됩니다.")
    public ApiResponse<VideoUpdateResDTO> updateVideo(
            @PathVariable @Positive Long projectId,
            @PathVariable @Positive Long videoId,
            @Valid @RequestBody VideoUpdateReqDTO request
    ) {
        // TODO: 인증/인가 구현 후 JWT에서 memberId 추출하도록 변경
        Long memberId = 1L;
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.updateVideo(memberId, projectId, videoId, request)
        );
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "영상 삭제", description = "프로젝트에 등록된 영상을 삭제합니다.")
    public ApiResponse<VideoDeleteResDTO> deleteVideo(
            @PathVariable @Positive Long projectId,
            @PathVariable @Positive Long videoId
    ) {
        // TODO: 인증/인가 구현 후 JWT에서 memberId 추출하도록 변경
        Long memberId = 1L;
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.deleteVideo(memberId, projectId, videoId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "영상 등록", description = "프로젝트에 새로운 YouTube 영상을 등록합니다.")
    public ApiResponse<VideoCreateResDTO> createVideo(
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody VideoCreateReqDTO request
    ) {
        // TODO: 인증/인가 구현 후 JWT에서 memberId 추출하도록 변경
        Long memberId = 1L;
        return ApiResponse.success(
                CommonSuccessCode.CREATED,
                videoService.createVideo(memberId, projectId, request)
        );
    }

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
