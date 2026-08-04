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

@Getter
@Builder
public class RecruitmentDetailResponse {

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

    private String description;

    private String shootingPeriod;

    private String contact;

    private Integer viewCount;

    private Long applicantCount;

    // 원시 boolean 이면 Jackson 이 is 를 벗겨 bookmarked 로 직렬화한다. 래퍼여야 키 이름이 유지된다.
    private Boolean isBookmarked;

    private Boolean isMine;

    private Boolean hasApplied;

    private WriterSummary writer;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class WriterSummary {

        private Long id;

        private String nickname;

        private String profileImageUrl;

        private RoleName primaryRole;

        private RegionName location;
    }
}
