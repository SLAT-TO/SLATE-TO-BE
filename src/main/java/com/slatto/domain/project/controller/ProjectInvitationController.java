package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectInvitationAcceptResponse;
import com.slatto.domain.project.dto.ProjectInvitationAcceptRequest;
import com.slatto.domain.project.dto.ProjectInvitationCreateRequest;
import com.slatto.domain.project.dto.ProjectInvitationCreateResponse;
import com.slatto.domain.project.dto.ProjectInvitationDetailResponse;
import com.slatto.domain.project.service.ProjectInvitationService;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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

    @Operation(
        summary = "프로젝트 초대 링크 생성",
        description = """
            ADMIN 만 만들 수 있다. expirationPeriod 로 유효기간을 정하며 기본값은 72시간이다.
            원본 토큰은 응답의 inviteUrl 에만 담기고 서버에는 해시로 저장된다.
            응답을 잃으면 서버에서 원본 토큰을 되찾거나 같은 링크를 다시 받을 수 없고, 새로 만들어야 한다."""
    )
    @ApiErrorCodes({"PROJECT403", "PROJECT_ADMIN403", "PROJECT404"})
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

    @Operation(
        summary = "프로젝트 초대 링크 정보 조회",
        description = """
            링크를 받은 사람이 로그인 전에 어떤 프로젝트인지 확인하는 용도다.
            status 로 PENDING, ACCEPTED, EXPIRED 를 구분한다."""
    )
    @SecurityRequirements
    @ApiErrorCodes("PROJECT_INVITATION404")
    @GetMapping("/project-invitations/{token}")
    public ApiResponse<ProjectInvitationDetailResponse> getInvitation(
        @PathVariable String token
    ) {
        ProjectInvitationDetailResponse response = projectInvitationService.getInvitation(token);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 초대 수락",
        description = """
            수락할 때 맡을 역할을 함께 보낸다.
            한 번 수락한 링크는 다시 쓸 수 없고, 기간이 지났거나 이미 멤버인 경우에도 실패한다."""
    )
    @ApiErrorCodes({"PROJECT_INVITATION404", "PROJECT_INVITATION409", "PROJECT_MEMBER409"})
    @PostMapping("/project-invitations/{token}/accept")
    public ApiResponse<ProjectInvitationAcceptResponse> acceptInvitation(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable String token,
        @Valid @RequestBody ProjectInvitationAcceptRequest request
    ) {
        ProjectInvitationAcceptResponse response = projectInvitationService.acceptInvitation(
            token,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
