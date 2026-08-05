package com.slatto.domain.recruitment.dto;

import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecruitmentApplicationResponse {

    private Long applicationId;

    private Long recruitmentId;

    private RecruitmentApplicationStatus applicationStatus;

    private String message;

    private String referenceLink;

    private LocalDateTime appliedAt;
}
