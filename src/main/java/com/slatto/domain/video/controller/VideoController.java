package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.request.VideoRequest.VideoCreateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoBookmarkUpdateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoUpdateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoBookmarkUpdateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoListResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoUpdateResDTO;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Video", description = "프로젝트 영상 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/videos")
public class VideoController {

    private final VideoService videoService;

    @PatchMapping("/{videoId}/bookmark")
    @Operation(
            summary = "영상 북마크 상태 변경",
            description = "프로젝트 멤버가 영상의 북마크 상태를 변경합니다. bookmarked에 원하는 상태를 전달하며, " +
                    "이미 같은 상태인 경우에도 성공으로 처리합니다."
    )
    public ApiResponse<VideoBookmarkUpdateResDTO> updateBookmark(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable @Positive Long projectId,
            @Parameter(description = "북마크 상태를 변경할 영상 ID", example = "1")
            @PathVariable @Positive Long videoId,
            @Valid @RequestBody VideoBookmarkUpdateReqDTO request
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.updateBookmark(memberId, projectId, videoId, request)
        );
    }

    @PatchMapping("/{videoId}")
    @Operation(summary = "영상 수정",
            description = "프로젝트 멤버가 영상의 제목과 메모를 수정합니다. " +
                    "변경하지 않을 값은 생략하거나 null로 전달할 수 있으며, 제목과 메모 중 하나 이상은 입력해야 합니다.")
    public ApiResponse<VideoUpdateResDTO> updateVideo(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable @Positive Long projectId,
            @Parameter(description = "영상 ID", example = "1")
            @PathVariable @Positive Long videoId,
            @Valid @RequestBody VideoUpdateReqDTO request
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.updateVideo(memberId, projectId, videoId, request)
        );
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "영상 삭제", description = "프로젝트 멤버가 프로젝트에 등록된 영상을 삭제합니다.")
    public ApiResponse<VideoDeleteResDTO> deleteVideo(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable @Positive Long projectId,
            @Parameter(description = "영상 ID", example = "1")
            @PathVariable @Positive Long videoId
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.deleteVideo(memberId, projectId, videoId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "영상 등록",
            description = "프로젝트 멤버가 YouTube 영상을 등록합니다. YouTube Data API로 공개 여부와 재생 가능 여부를 확인하며, " +
                    "같은 프로젝트에 이미 등록된 영상이나 비공개·재생 불가능 영상은 등록할 수 없습니다."
    )
    public ApiResponse<VideoCreateResDTO> createVideo(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody VideoCreateReqDTO request
    ) {
        return ApiResponse.success(
                CommonSuccessCode.CREATED,
                videoService.createVideo(memberId, projectId, request)
        );
    }

    @GetMapping
    @Operation(
            summary = "영상 목록 조회",
            description = "프로젝트 멤버가 등록 영상을 최신순으로 조회합니다. 첫 요청에서는 cursor를 생략하고, " +
                    "다음 페이지는 이전 응답의 nextCursor를 전달합니다. size 기본값은 20이며 최대 100입니다."
    )
    public ApiResponse<VideoListResDTO> getVideos(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable @Positive Long projectId,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "9")
            @RequestParam(required = false) @Positive Long cursor,
            @Parameter(description = "조회 개수. 생략 시 20, 최대 100입니다.", example = "20")
            @RequestParam(required = false) @Min(1) @Max(100) Integer size
    ) {
        return ApiResponse.success(
                CommonSuccessCode.OK,
                videoService.getVideos(memberId, projectId, cursor, size)
        );
    }
}
