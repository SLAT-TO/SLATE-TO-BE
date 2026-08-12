package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectFileDownloadResponse;
import com.slatto.domain.project.dto.ProjectFileListResponse;
import com.slatto.domain.project.dto.ProjectFilePinResponse;
import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileUpdateRequest;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.service.ProjectFileService;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "Project File", description = "프로젝트 파일 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/files")
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    @Operation(
        summary = "프로젝트 파일 목록 조회",
        description = """
            고정된 파일이 먼저 오고, keyword 로 파일명을 검색할 수 있다.
            cursor 기반 페이지네이션이며 size 는 기본 20, 최대 50이다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @GetMapping
    public ApiResponse<ProjectFileListResponse> getProjectFiles(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @Parameter(description = "파일명 검색어. 생략하면 전체를 조회합니다.", example = "콘티")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "18")
        @RequestParam(required = false) Long cursor,
        @Parameter(description = "조회 개수. 생략 시 20, 최대 50입니다.", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        ProjectFileListResponse response = projectFileService.getProjectFiles(
            projectId,
            currentUserId,
            keyword,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 파일 업로드",
        description = """
            multipart/form-data 로 보낸다. 최대 100MB 이며 pdf, jpg, jpeg, png, doc, docx 만 허용한다.
            확장자와 Content-Type 이 서로 맞지 않으면 거부한다. 업로드하면 다른 멤버에게 알림이 간다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectFileResponse> uploadProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestPart("request") ProjectFileUploadRequest request,
        @RequestPart("file") MultipartFile file
    ) {
        ProjectFileResponse response = projectFileService.uploadProjectFile(
            projectId,
            currentUserId,
            request,
            file
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "프로젝트 파일 수정",
        description = "업로더 본인 또는 ADMIN 만 수정할 수 있다. 보내지 않은 필드는 기존 값이 유지된다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @PatchMapping("/{fileId}")
    public ApiResponse<ProjectFileResponse> updateProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @Valid @RequestBody ProjectFileUpdateRequest request
    ) {
        ProjectFileResponse response = projectFileService.updateProjectFile(
            projectId,
            fileId,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 파일 삭제",
        description = """
            업로더 본인 또는 ADMIN 만 삭제할 수 있다.
            삭제 표시만 남기며 저장소의 파일 자체는 지우지 않는다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId
    ) {
        projectFileService.deleteProjectFile(projectId, fileId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "프로젝트 파일 고정",
        description = """
            파일 고정은 프로젝트 멤버 모두에게 함께 보인다. 개인별로 적용되는 프로젝트 고정과 다르다.
            업로더 본인 또는 ADMIN 만 할 수 있다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @PostMapping("/{fileId}/pin")
    public ApiResponse<ProjectFilePinResponse> pinProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId
    ) {
        ProjectFilePinResponse response = projectFileService.pinProjectFile(
            projectId,
            fileId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 파일 고정 해제",
        description = "업로더 본인 또는 ADMIN 만 할 수 있다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @DeleteMapping("/{fileId}/pin")
    public ApiResponse<ProjectFilePinResponse> unpinProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId
    ) {
        ProjectFilePinResponse response = projectFileService.unpinProjectFile(
            projectId,
            fileId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 파일 다운로드",
        description = "공통 응답 래퍼가 아니라 파일 본문을 그대로 반환한다. Content-Disposition 이 attachment 로 내려간다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_FILE404"})
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId
    ) {
        ProjectFileDownloadResponse response = projectFileService.downloadProjectFile(
            projectId,
            fileId,
            currentUserId
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(response.getContentType()))
            .contentLength(response.getFileSize())
            .headers(headers -> headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(response.getFileName(), StandardCharsets.UTF_8)
                    .build()
            ))
            .body(new InputStreamResource(response.getInputStream()));
    }
}
