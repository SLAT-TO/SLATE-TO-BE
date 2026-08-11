package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectMemberDetailResponse;
import com.slatto.domain.project.dto.ProjectMemberListResponse;
import com.slatto.domain.project.dto.ProjectMemberUpdateRequest;
import com.slatto.domain.project.service.ProjectMemberService;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Member", description = "프로젝트 멤버 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(
        summary = "프로젝트 멤버 목록 조회",
        description = "페이지네이션 없이 전체를 반환한다. 프로젝트를 나간 멤버는 제외된다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @GetMapping
    public ApiResponse<ProjectMemberListResponse> getProjectMembers(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        ProjectMemberListResponse response = projectMemberService.getProjectMembers(
            projectId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 나가기",
        description = "ADMIN 은 나갈 수 없다. 나가면 멤버 목록에서 빠지지만 작성한 글과 파일은 남는다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404"})
    @DeleteMapping("/me")
    public ApiResponse<Void> leaveProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        projectMemberService.leaveProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "프로젝트 멤버 상세 조회",
        description = "경로의 memberId 는 사용자 ID 가 아니라 프로젝트 멤버 ID 다."
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_MEMBER404"})
    @GetMapping("/{memberId}")
    public ApiResponse<ProjectMemberDetailResponse> getProjectMember(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long memberId
    ) {
        ProjectMemberDetailResponse response = projectMemberService.getProjectMember(
            projectId,
            memberId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 멤버 역할 수정",
        description = """
            ADMIN 이거나 본인의 역할일 때만 수정할 수 있다.
            보낸 역할 목록으로 전체를 교체하므로, 유지할 역할도 함께 보내야 한다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT404", "PROJECT_MEMBER404"})
    @PatchMapping("/{memberId}")
    public ApiResponse<ProjectMemberDetailResponse> updateProjectMemberRoles(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @Valid @RequestBody ProjectMemberUpdateRequest request
    ) {
        ProjectMemberDetailResponse response = projectMemberService.updateProjectMemberRoles(
            projectId,
            memberId,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 멤버 삭제",
        description = """
            ADMIN 만 다른 멤버를 내보낼 수 있다.
            자기 자신은 이 API 로 내보낼 수 없고 나가기를 써야 한다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT_ADMIN403", "PROJECT404", "PROJECT_MEMBER404"})
    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> removeProjectMember(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long memberId
    ) {
        projectMemberService.removeProjectMember(projectId, memberId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
