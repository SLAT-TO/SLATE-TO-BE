package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectFileDownloadResponse;
import com.slatto.domain.project.dto.ProjectFileListResponse;
import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileUpdateRequest;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.service.ProjectFileService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "프로젝트 파일 목록 조회")
    @GetMapping
    public ApiResponse<ProjectFileListResponse> getProjectFiles(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long cursor,
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

    @Operation(summary = "프로젝트 파일 업로드")
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

    @Operation(summary = "프로젝트 파일 수정")
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

    @Operation(summary = "프로젝트 파일 삭제")
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteProjectFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long fileId
    ) {
        projectFileService.deleteProjectFile(projectId, fileId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(summary = "프로젝트 파일 다운로드")
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
