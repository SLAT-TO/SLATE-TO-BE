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

    @Operation(summary = "프로젝트 목록 조회")
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

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> createProject(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectResponse response = projectService.createProject(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "프로젝트 상세 조회")
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
            `status` 를 `COMPLETED` 로 바꾸면 참여 중인 멤버 전원의 포트폴리오에 이 프로젝트가 생성된다.
            프로젝트명·유형·개인외주 구분·설명·기간이 그대로 옮겨가고, 각자 맡은 역할이 함께 채워진다.
            생성된 뒤에는 본인이 프로필에서 수정·삭제할 수 있다.

            나간 멤버와 탈퇴한 유저는 대상에서 빠진다.

            `COMPLETED` 는 최종 상태다. 완료한 뒤에는 다른 단계로 되돌릴 수 없고
            시도하면 `PROJECT_COMPLETED409` 가 나간다. 이력이 두 번 생기는 것을 막기 위해서다.

            `title` 또는 `kind` 가 비어 있으면 포트폴리오를 만들 수 없어 완료로 바꿀 수 없다.
            이때는 `PROJECT_COMPLETION400` 이 나간다.
            `title` 은 생성·수정 요청 모두 필수라 실제로는 `kind` 만 이 조건에 걸린다.
            """
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

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        projectService.deleteProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(summary = "프로젝트 고정")
    @PostMapping("/{projectId}/pin")
    public ApiResponse<ProjectPinResponse> pinProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectPinResponse response = projectService.pinProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 고정 해제")
    @DeleteMapping("/{projectId}/pin")
    public ApiResponse<ProjectPinResponse> unpinProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectPinResponse response = projectService.unpinProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
