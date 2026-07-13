package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectResponse;
import com.slatto.domain.project.dto.ProjectUpdateRequest;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.service.ProjectService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project", description = "프로젝트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private static final String CURRENT_USER_ID_HEADER = "X-USER-ID";

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 목록 조회")
    @GetMapping
    public ApiResponse<ProjectListResponse> getProjects(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @RequestParam(required = false) ProjectStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProjectListResponse response = projectService.getProjects(currentUserId, status, cursor, size);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> createProject(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectResponse response = projectService.createProject(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProject(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectDetailResponse response = projectService.getProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 수정")
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResponse> updateProject(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectUpdateRequest request
    ) {
        ProjectResponse response = projectService.updateProject(projectId, currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId
    ) {
        projectService.deleteProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
