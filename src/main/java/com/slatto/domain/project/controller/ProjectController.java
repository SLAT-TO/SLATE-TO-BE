package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectCreateRequest;
import com.slatto.domain.project.dto.ProjectDetailResponse;
import com.slatto.domain.project.dto.ProjectListResponse;
import com.slatto.domain.project.dto.ProjectPinResponse;
import com.slatto.domain.project.dto.ProjectResponse;
import com.slatto.domain.project.dto.ProjectUpdateRequest;
import com.slatto.domain.project.enums.ProjectStatus;
import com.slatto.domain.project.service.ProjectService;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        @Parameter(description = "진행 단계로 거릅니다. PREPARING, EDITING, REVIEWING, COMPLETED 중 하나이며 생략하면 전체를 조회합니다.", example = "EDITING")
        @RequestParam(required = false) ProjectStatus status,
        @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "12")
        @RequestParam(required = false) Long cursor,
        @Parameter(description = "조회 개수. 생략 시 20, 최대 50입니다.", example = "20")
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
    @ApiErrorCodes("PROJECT409")
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
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
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
            보내지 않은 필드는 null 로 덮어써지므로, 바꾸지 않을 값도 함께 보내야 한다.

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
    @ApiErrorCodes({"PROJECT403", "PROJECT_ADMIN403", "PROJECT404", "PROJECT_COMPLETED409"})
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
    @ApiErrorCodes({"PROJECT403", "PROJECT_ADMIN403", "PROJECT404"})
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
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
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
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @DeleteMapping("/{projectId}/pin")
    public ApiResponse<ProjectPinResponse> unpinProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectPinResponse response = projectService.unpinProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
