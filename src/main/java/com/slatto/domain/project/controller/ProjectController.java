package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectPinResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project", description = "프로젝트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
        summary = "프로젝트 목록 조회",
        description = """
            내가 참여 중인 프로젝트만 반환한다. 내가 고정한 프로젝트가 먼저 오고, 나머지는 최근에 만들어진 순이다.
            cursor 기반 페이지네이션이며 size 는 기본 20, 최대 50이다."""
    )
    @GetMapping
    public ApiResponse<ProjectListResponse> getProjects(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) ProjectStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProjectListResponse response = projectService.getProjects(currentUserId, status, cursor, size);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 생성",
        description = """
            만든 사람이 ADMIN 역할의 멤버로 함께 등록된다.
            한 사람이 가질 수 있는 프로젝트는 5개까지이며, 삭제한 프로젝트는 개수에 포함되지 않는다."""
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> createProject(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectResponse response = projectService.createProject(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "프로젝트 상세 조회",
        description = "프로젝트 멤버만 조회할 수 있다."
    )
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectDetailResponse response = projectService.getProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 수정",
        description = """
            ADMIN 만 수정할 수 있다.
            보내지 않은 필드는 null 로 덮어써지므로, 바꾸지 않을 값도 함께 보내야 한다."""
    )
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResponse> updateProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectUpdateRequest request
    ) {
        ProjectResponse response = projectService.updateProject(projectId, currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 삭제",
        description = "ADMIN 만 삭제할 수 있다. 실제로 지우지 않고 삭제 표시만 남긴다."
    )
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        projectService.deleteProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "프로젝트 고정",
        description = """
            고정은 호출한 사람에게만 적용되며 다른 멤버의 목록 순서에는 영향을 주지 않는다.
            이미 고정한 프로젝트에 다시 호출해도 실패하지 않는다."""
    )
    @PostMapping("/{projectId}/pin")
    public ApiResponse<ProjectPinResponse> pinProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectPinResponse response = projectService.pinProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 고정 해제",
        description = "고정하지 않은 프로젝트에 호출해도 실패하지 않는다."
    )
    @DeleteMapping("/{projectId}/pin")
    public ApiResponse<ProjectPinResponse> unpinProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectPinResponse response = projectService.unpinProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
