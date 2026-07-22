package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.service.ProjectFileService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Project File", description = "프로젝트 파일 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/files")
public class ProjectFileController {

    private final ProjectFileService projectFileService;

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
}
