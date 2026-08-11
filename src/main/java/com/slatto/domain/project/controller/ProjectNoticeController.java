package com.slatto.domain.project.controller;

import com.slatto.domain.project.dto.ProjectNoticeCreateRequest;
import com.slatto.domain.project.dto.ProjectNoticeListResponse;
import com.slatto.domain.project.dto.ProjectNoticeReadResponse;
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

@Tag(name = "Project Notice", description = "프로젝트 공지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/notices")
public class ProjectNoticeController {

    private final ProjectNoticeService projectNoticeService;

    @Operation(
        summary = "프로젝트 공지 목록 조회",
        description = """
            각 항목에 내가 읽었는지 여부가 함께 담긴다.
            cursor 기반 페이지네이션이며 size 는 기본 20, 최대 50이다."""
    )
    @GetMapping
    public ApiResponse<ProjectNoticeListResponse> getProjectNotices(
        @AuthenticationPrincipal Long currentUserId,
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

    @Operation(
        summary = "프로젝트 공지 상세 조회",
        description = "조회만으로는 읽음 처리되지 않는다. 읽음 처리는 별도 엔드포인트를 호출해야 한다."
    )
    @GetMapping("/{noticeId}")
    public ApiResponse<ProjectNoticeResponse> getProjectNotice(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long noticeId
    ) {
        ProjectNoticeResponse response = projectNoticeService.getProjectNotice(
            projectId,
            noticeId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "프로젝트 공지 등록",
        description = """
            프로젝트 멤버면 누구나 등록할 수 있다.
            작성자 본인은 처음부터 읽음 상태이며, 나머지 멤버에게는 알림이 간다."""
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectNoticeResponse> createProjectNotice(
        @AuthenticationPrincipal Long currentUserId,
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

    @Operation(
        summary = "프로젝트 공지 수정",
        description = "작성자 본인 또는 ADMIN 만 수정할 수 있다. 제목과 내용을 모두 덮어쓴다."
    )
    @PatchMapping("/{noticeId}")
    public ApiResponse<ProjectNoticeResponse> updateProjectNotice(
        @AuthenticationPrincipal Long currentUserId,
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

    @Operation(
        summary = "프로젝트 공지 삭제",
        description = "작성자 본인 또는 ADMIN 만 삭제할 수 있다. 삭제 표시만 남긴다."
    )
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteProjectNotice(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long noticeId
    ) {
        projectNoticeService.deleteProjectNotice(projectId, noticeId, currentUserId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }

    @Operation(
        summary = "프로젝트 공지 읽음 처리",
        description = """
            읽음은 호출한 사람에게만 기록된다.
            이미 읽은 공지에 다시 호출해도 실패하지 않고, 읽은 시각만 갱신된다."""
    )
    @PatchMapping("/{noticeId}/read")
    public ApiResponse<ProjectNoticeReadResponse> readProjectNotice(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long noticeId
    ) {
        ProjectNoticeReadResponse response = projectNoticeService.readProjectNotice(
            projectId,
            noticeId,
            currentUserId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
