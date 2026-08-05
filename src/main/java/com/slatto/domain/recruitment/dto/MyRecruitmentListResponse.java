package com.slatto.domain.recruitment.dto;

import com.slatto.domain.project.enums.LengthType;
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
public class MyRecruitmentListResponse {

    private List<MyRecruitmentSummary> items;

    // 공고 id 기준이다.
    private Long nextCursor;

    private Boolean hasNext;

    @Getter
    @Builder
    public static class MyRecruitmentSummary {

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

        private Long applicantCount;

        private Boolean isBookmarked;

        private Boolean isMine;

        private RecruitmentSummary.WriterSummary writer;

        private LocalDateTime createdAt;
    }
}
