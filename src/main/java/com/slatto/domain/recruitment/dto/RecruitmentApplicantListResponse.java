package com.slatto.domain.recruitment.dto;

import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecruitmentApplicantListResponse {

    private List<ApplicantSummary> items;

    // 지원 id(applicationId) 기준이다.
    private Long nextCursor;

    private Boolean hasNext;

    @Getter
    @Builder
    public static class ApplicantSummary {

        private Long applicationId;

        private RecruitmentApplicationStatus applicationStatus;

        private String message;

        private String referenceLink;

        private LocalDateTime appliedAt;

        private ApplicantProfile applicant;
    }

    // 지원자 카드에서만 primaryRole 과 location 을 노출한다. 반드시 배치 조회로 채운다.
    @Getter
    @Builder
    public static class ApplicantProfile {

        private Long id;

        private String nickname;

        private String profileImageUrl;

        private RoleName primaryRole;

        private RegionName location;
    }
}
