package com.slatto.domain.recruitment.controller;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentRecommendationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.enums.RecruitmentSortType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.recruitment.service.RecruitmentService;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
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

import java.util.List;

@Tag(name = "Recruitment", description = "구인구직 공고 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @Operation(
        summary = "구인구직 공고 작성",
        description = """
            필수는 `title`, `recruitPart`, `description` 세 개다.
            `category`, `lengthType`, `location`, `shootingPeriod`, `pay`, `contact`, `deadline` 은 비워도 등록된다.
            다만 값을 보낼 거면 공백만 채워 보낼 수는 없다.

            `title` 은 5~50자다. `deadline` 을 비우면 수동으로 마감할 때까지 모집이 유지된다.
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentDetailResponse> createRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @Valid @RequestBody RecruitmentCreateRequest request
    ) {
        RecruitmentDetailResponse response = recruitmentService.createRecruitment(currentUserId, request);

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "구인구직 공고 목록 조회",
        description = """
            필터는 AND 로 결합한다. nextCursor 는 공고 ID 이며 프론트는 해석하지 않고 그대로 되돌려준다.
            정렬이나 필터가 바뀌면 cursor 를 버리고 첫 페이지부터 다시 조회해야 한다.

            `category`, `recruitPart`, `location` 은 복수 선택이다. 같은 키를 반복해서 보낸다.

            ```
            GET /api/v1/recruitments?location=SEOUL&location=GYEONGGI&recruitPart=DIRECTOR
            ```

            같은 키 안의 값들은 OR 로, 서로 다른 키는 AND 로 묶인다. 위 예시는
            (서울 또는 경기) 이면서 연출 파트인 공고다. 값을 하나도 보내지 않으면 해당 필터는 적용되지 않는다.
            """
    )
    @GetMapping
    public ApiResponse<RecruitmentListResponse> getRecruitments(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) List<CategoryName> category,
        @RequestParam(required = false) LengthType lengthType,
        @RequestParam(required = false) List<RoleName> recruitPart,
        @RequestParam(required = false) List<RegionName> location,
        @RequestParam(required = false) RecruitmentStatus status,
        @RequestParam(defaultValue = "LATEST") RecruitmentSortType sort,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        RecruitmentListResponse response = recruitmentService.getRecruitments(
            currentUserId,
            keyword,
            category,
            lengthType,
            recruitPart,
            location,
            status,
            sort,
            cursor,
            size
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 추천 공고 조회",
        description = "활동 역할·관심 카테고리·활동 지역 매칭도 상위 공고 중에서 무작위로 고른다. "
            + "호출마다 구성이 달라질 수 있으나 같은 조합이 다시 나올 수 있고, "
            + "대상 공고가 요청 개수 이하이면 항상 같은 공고가 반환된다. 페이지네이션이 없다."
    )
    @GetMapping("/recommended")
    public ApiResponse<RecruitmentRecommendationResponse> getRecommendedRecruitments(
        @AuthenticationPrincipal Long currentUserId,
        @RequestParam(defaultValue = "4") int size
    ) {
        RecruitmentRecommendationResponse response =
            recruitmentService.getRecommendedRecruitments(currentUserId, size);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "구인구직 공고 상세 조회")
    @GetMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> getRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        RecruitmentDetailResponse response = recruitmentService.getRecruitment(currentUserId, recruitmentId);

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(
        summary = "구인구직 공고 수정",
        description = """
            전달한 항목만 부분 수정된다.

            **마감된 공고는 내용을 수정할 수 없다.** 마감일이 지났거나 수동 마감한 공고에
            내용 수정을 요청하면 `RECRUITMENT_CLOSED_EDIT400` 이 반환된다.

            다만 `status` 를 `RECRUITING` 으로 보내는 요청은 통과한다. 상태 변경이 같은 API 라
            전면 차단하면 마감을 되돌릴 방법이 없어지기 때문이다. 마감일이 지나 자동 마감된
            공고를 되살리려면 `deadline` 도 함께 보내야 한다.
            """
    )
    @PatchMapping("/{recruitmentId}")
    public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @Valid @RequestBody RecruitmentUpdateRequest request
    ) {
        RecruitmentDetailResponse response = recruitmentService.updateRecruitment(
            currentUserId,
            recruitmentId,
            request
        );

        return ApiResponse.success(CommonSuccessCode.OK, response);
    }

    @Operation(summary = "구인구직 공고 삭제")
    @DeleteMapping("/{recruitmentId}")
    public ApiResponse<Void> deleteRecruitment(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId
    ) {
        recruitmentService.deleteRecruitment(currentUserId, recruitmentId);

        return ApiResponse.success(CommonSuccessCode.OK, null);
    }
}
