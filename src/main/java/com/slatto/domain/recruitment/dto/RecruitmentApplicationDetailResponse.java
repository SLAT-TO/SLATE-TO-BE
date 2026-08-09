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

    @Getter
    @Builder
    public static class ApplicantProfile {

        private Long id;

        private String nickname;

        private String profileImageUrl;

        private RoleName primaryRole;

        private List<RegionName> locations;
    }
}
