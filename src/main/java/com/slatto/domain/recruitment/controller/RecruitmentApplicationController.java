package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.RecruitmentApplicantListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationStatusUpdateRequest;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.service.RecruitmentApplicationService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment Application", description = "구인구직 공고 지원 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments/{recruitmentId}/applications")
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService recruitmentApplicationService;

    @Operation(
        summary = "구인구직 공고 지원",
        description = """
            `fileIds` 는 첨부 파일 업로드 API 가 돌려준 id 목록이다. 최대 10개이며 생략할 수 있다.

            본인이 업로드했고, 이 공고에 올렸고, 아직 다른 지원에 쓰이지 않은 파일만 붙일 수 있다.
            하나라도 조건에 맞지 않으면 `APPLICATION_FILE_LINK400` 으로 지원 전체가 실패한다.
            첨부를 의도한 지원이 첨부 없이 접수되면 지원자는 성공 응답을 받고도 서류가 빠진 상태가 되기 때문이다.
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentApplicationResponse> applyToRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @Valid @RequestBody RecruitmentApplicationCreateRequest request
    ) {
        RecruitmentApplicationResponse response = recruitmentApplicationService.applyToRecruitment(
            currentUserId,
            recruitmentId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "구인구직 공고 지원자 목록 조회",
        description = "공고 작성자만 조회할 수 있다. nextCursor 는 지원 ID 기준이다."
    )
    @GetMapping
    public ApiResponse<RecruitmentApplicantListResponse> getApplicants(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @RequestParam(required = false) RecruitmentApplicationStatus status,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecruitmentApplicantListResponse response = recruitmentApplicationService.getApplicants(
            currentUserId,
            recruitmentId,
            status,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 공고 지원 상세 조회",
        description = """
            공고 작성자와 지원 본인만 조회할 수 있다. 그 외에는 403 이다.

            지원자 프로필, 자기소개(`message`), 참고 링크, 첨부 파일 목록을 함께 반환한다.
            `files` 에는 파일 본문이 아니라 메타데이터만 담긴다.
            실제 내려받기는 첨부 파일 다운로드 API 를 쓴다.
            """
    )
    @GetMapping("/{applicationId}")
    public ApiResponse<RecruitmentApplicationDetailResponse> getApplicationDetail(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @PathVariable Long applicationId
    ) {
        RecruitmentApplicationDetailResponse response = recruitmentApplicationService.getApplicationDetail(
            currentUserId,
            recruitmentId,
            applicationId
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 공고 지원 상태 변경",
        description = "공고 작성자만 변경할 수 있다. PENDING 상태의 지원만 ACCEPTED 또는 REJECTED 로 바꿀 수 있다."
    )
    @PatchMapping("/{applicationId}")
    public ApiResponse<Void> changeApplicationStatus(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitmentApplicationStatusUpdateRequest request
    ) {
        recruitmentApplicationService.changeApplicationStatus(
            currentUserId,
            recruitmentId,
            applicationId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
