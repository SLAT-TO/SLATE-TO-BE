package com.slatto.domain.video.controller;

import com.slatto.domain.video.dto.request.VideoRequest.VideoReferenceFileCreateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileListResDTO;
import com.slatto.domain.video.service.VideoReferenceFileService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Video Reference File", description = "영상 참조 파일 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/videos/{videoId}/reference-files")
public class VideoReferenceFileController {

    private final VideoReferenceFileService videoReferenceFileService;

    @GetMapping
    @Operation(
        summary = "영상 참조 파일 목록 조회",
        description = "프로젝트 멤버가 영상에 연결된 참조 파일을 조회합니다. keyword로 파일명을 검색할 수 있으며, " +
            "첫 요청에서는 cursor를 생략하고 다음 페이지는 이전 응답의 nextCursor를 전달합니다."
    )
    public ApiResponse<VideoReferenceFileListResDTO> getReferenceFiles(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "프로젝트 ID", example = "10")
        @PathVariable @Positive Long projectId,
        @Parameter(description = "영상 ID", example = "1")
        @PathVariable @Positive Long videoId,
        @Parameter(description = "파일명 검색어", example = "reference")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "9")
        @RequestParam(required = false) @Positive Long cursor,
        @Parameter(description = "조회 개수. 생략 시 20, 최대 100입니다.", example = "20")
        @RequestParam(required = false) @Min(1) @Max(100) Integer size
    ) {
        VideoReferenceFileListResDTO response = videoReferenceFileService.getReferenceFiles(
            currentUserId,
            projectId,
            videoId,
            keyword,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "영상 참조 파일 연결",
        description = "기존 프로젝트 파일을 영상 참조 파일로 연결합니다. 파일 업로드는 프로젝트 파일 API에서 먼저 처리합니다."
    )
    public ApiResponse<VideoReferenceFileCreateResDTO> createReferenceFile(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "프로젝트 ID", example = "10")
        @PathVariable @Positive Long projectId,
        @Parameter(description = "영상 ID", example = "1")
        @PathVariable @Positive Long videoId,
        @Valid @RequestBody VideoReferenceFileCreateReqDTO request
    ) {
        VideoReferenceFileCreateResDTO response = videoReferenceFileService.createReferenceFile(
            currentUserId,
            projectId,
            videoId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping("/{referenceFileId}")
    @Operation(
        summary = "영상 참조 파일 연결 제거",
        description = "영상과 프로젝트 파일의 참조 연결만 제거합니다. 원본 프로젝트 파일과 S3 파일은 삭제하지 않습니다."
    )
    public ApiResponse<VideoReferenceFileDeleteResDTO> deleteReferenceFile(
        @AuthenticationPrincipal Long currentUserId,
        @Parameter(description = "프로젝트 ID", example = "10")
        @PathVariable @Positive Long projectId,
        @Parameter(description = "영상 ID", example = "1")
        @PathVariable @Positive Long videoId,
        @Parameter(description = "영상 참조 파일 ID", example = "5")
        @PathVariable @Positive Long referenceFileId
    ) {
        VideoReferenceFileDeleteResDTO response = videoReferenceFileService.deleteReferenceFile(
            currentUserId,
            projectId,
            videoId,
            referenceFileId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
