package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectMemberDetailResponse;
import com.slatto.domain.project.dto.ProjectMemberListResponse;
import com.slatto.domain.project.dto.ProjectMemberUpdateRequest;
import com.slatto.domain.project.service.ProjectMemberService;
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

    @Operation(summary = "프로젝트 멤버 목록 조회")
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

    @Operation(summary = "프로젝트 나가기")
    @DeleteMapping("/me")
    public ApiResponse<Void> leaveProject(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId
    ) {
        projectMemberService.leaveProject(projectId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(summary = "프로젝트 멤버 상세 조회")
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

    @Operation(summary = "프로젝트 멤버 역할 수정")
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

    @Operation(summary = "프로젝트 멤버 삭제")
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
