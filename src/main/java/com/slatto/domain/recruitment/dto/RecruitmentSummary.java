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

// 전체 목록/추천/관심 목록이 공유하는 공고 카드다.
// writer.primaryRole 과 location 은 유저당 쿼리 2방이라 카드에 넣지 않는다. 상세 응답에만 있다.
// contact 도 넣지 않는다. 목록 API 로 연락처가 무차별 노출된다.
@Getter
@Builder
public class RecruitmentSummary {

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

    // 원시 boolean 이면 Jackson 이 is 를 벗겨 bookmarked 로 직렬화한다. 래퍼여야 키 이름이 유지된다.
    private Boolean isBookmarked;

    private Boolean isMine;

    private WriterSummary writer;

    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class WriterSummary {

        private Long id;

        private String nickname;

        private String profileImageUrl;
    }
}
