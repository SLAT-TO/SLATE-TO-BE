package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectNoticeCreateRequest;
import com.slatto.domain.project.dto.ProjectNoticeListResponse;
import com.slatto.domain.project.dto.ProjectNoticeResponse;
import com.slatto.domain.project.dto.ProjectNoticeUpdateRequest;
import com.slatto.domain.project.service.ProjectNoticeService;
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

@Tag(name = "Project Notice", description = "프로젝트 공지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/notices")
public class ProjectNoticeController {

    private static final String CURRENT_USER_ID_HEADER = "X-USER-ID";

    private final ProjectNoticeService projectNoticeService;

    @Operation(summary = "프로젝트 공지 목록 조회")
    @GetMapping
    public ApiResponse<ProjectNoticeListResponse> getProjectNotices(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProjectNoticeListResponse response = projectNoticeService.getProjectNotices(
            projectId,
            currentUserId,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 공지 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectNoticeResponse> createProjectNotice(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectNoticeCreateRequest request
    ) {
        ProjectNoticeResponse response = projectNoticeService.createProjectNotice(
            projectId,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(summary = "프로젝트 공지 수정")
    @PatchMapping("/{noticeId}")
    public ApiResponse<ProjectNoticeResponse> updateProjectNotice(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long noticeId,
        @Valid @RequestBody ProjectNoticeUpdateRequest request
    ) {
        ProjectNoticeResponse response = projectNoticeService.updateProjectNotice(
            projectId,
            noticeId,
            currentUserId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "프로젝트 공지 삭제")
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteProjectNotice(
        @RequestHeader(CURRENT_USER_ID_HEADER) Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long noticeId
    ) {
        projectNoticeService.deleteProjectNotice(projectId, noticeId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
