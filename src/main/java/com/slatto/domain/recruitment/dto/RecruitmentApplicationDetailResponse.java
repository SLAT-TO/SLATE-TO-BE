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

    // 공개 프로필(GET /users/{userId}) 항목에 이메일을 더한 것이다.
    // 이메일은 공개 프로필에 없다. 수락한 지원자에게 연락할 수단이 필요해 여기에만 담으며,
    // 이 응답은 공고 작성자와 지원 본인만 열 수 있어 노출 범위가 제한된다.
    // 목록 응답에는 넣지 않는다. 한 번의 조회로 지원자 전원의 이메일이 넘어가기 때문이다.
    @Getter
    @Builder
    public static class ApplicantProfile {

        private Long id;

        private String nickname;

        private String email;

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
