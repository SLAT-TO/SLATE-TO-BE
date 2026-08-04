package com.slatto.domain.recruitment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 전체 공고 목록과 관심 있는 공고 목록이 공유한다.
// nextCursor 기준이 다르다 — 전체 목록은 공고 id, 관심 목록은 북마크 id 다.
@Getter
@Builder
public class RecruitmentListResponse {

    private List<RecruitmentSummary> items;

    private Long nextCursor;

    private Boolean hasNext;
}
