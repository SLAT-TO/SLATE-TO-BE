package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectInvitationAcceptResponse;
import com.slatto.domain.project.dto.ProjectInvitationCreateRequest;
import com.slatto.domain.project.dto.ProjectInvitationCreateResponse;
import com.slatto.domain.project.dto.ProjectInvitationDetailResponse;
import com.slatto.domain.project.service.ProjectInvitationService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Invitation", description = "프로젝트 초대 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectInvitationController {

    private final ProjectInvitationService projectInvitationService;

    @Operation(summary = "프로젝트 초대 링크 생성")
    @PostMapping("/projects/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectInvitationCreateResponse> createInvitation(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody(required = false) ProjectInvitationCreateRequest request
    ) {
        ProjectInvitationCreateResponse response = projectInvitationService.createInvitation(
            projectId,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "프로젝트 초대 링크 정보 조회")
    @GetMapping("/project-invitations/{token}")
    public ApiResponse<ProjectInvitationDetailResponse> getInvitation(
        @PathVariable String token
    ) {
        ProjectInvitationDetailResponse response = projectInvitationService.getInvitation(token);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 초대 수락")
    @PostMapping("/project-invitations/{token}/accept")
    public ApiResponse<ProjectInvitationAcceptResponse> acceptInvitation(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable String token
    ) {
        ProjectInvitationAcceptResponse response = projectInvitationService.acceptInvitation(
            token,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
