package com.slatto.domain.recruitment.dto;

import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.user.dto.PortfolioListResponse;
import com.slatto.domain.user.dto.UserStatsResponse;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecruitmentApplicationDetailResponse {

    private Long applicationId;

    private Long recruitmentId;

    private RecruitmentApplicationStatus applicationStatus;

    private String message;

    private String referenceLink;

    private LocalDateTime appliedAt;

    private ApplicantProfile applicant;

    // 파일 본문은 이 응답에 담기지 않는다. 다운로드 API 로 개별 조회한다.
    private List<RecruitmentApplicationFileResponse> files;

    // 공개 프로필(GET /users/{userId})과 같은 항목을 담는다.
    // 지원자 프로필 화면이 이 응답 하나로 완성되도록 모아둔 것이라 노출 범위는 공개 프로필과 동일하다.
    @Getter
    @Builder
    public static class ApplicantProfile {

        private Long id;

        private String nickname;

        private String profileImageUrl;

        private String bio;

        private RoleName primaryRole;

        private List<RoleName> roles;

        private List<RegionName> locations;

        private List<CategoryName> categories;

        private UserStatsResponse stats;

        // 최신 몇 건만 담는다. 전체 목록은 GET /users/{userId}/portfolios 로 이어서 조회한다.
        private PortfolioListResponse portfolios;
    }
}
