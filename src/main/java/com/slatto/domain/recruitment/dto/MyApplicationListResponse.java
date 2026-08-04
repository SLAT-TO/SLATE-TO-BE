package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyApplicationListResponse {

    private List<MyApplicationSummary> items;

    // 공고 id 가 아니라 지원 id(applicationId) 기준이다.
    private Long nextCursor;

    private Boolean hasNext;

    @Getter
    @Builder
    public static class MyApplicationSummary {

        private Long applicationId;

        private RecruitmentApplicationStatus applicationStatus;

        private LocalDateTime appliedAt;

        // 이하 공고 카드. id 는 공고 id 다.
        private Long id;

        private String title;

        private CategoryName category;

        private LengthType lengthType;

        private RoleName recruitPart;

        private RegionName location;

        private String pay;

        private LocalDate deadline;

        private Integer dday;

        private RecruitmentStatus status;

        private Integer viewCount;

        private Boolean isBookmarked;

        private Boolean isMine;

        private RecruitmentSummary.WriterSummary writer;

        private LocalDateTime createdAt;
    }
}
