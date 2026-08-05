package com.slatto.domain.recruitment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 고정 개수 반환이라 페이지네이션이 없다. hasNext 를 억지로 내리면
// 프론트가 "다음 페이지 없음"과 "페이지네이션 미지원"을 구분하지 못한다.
@Getter
@Builder
public class RecruitmentRecommendationResponse {

    private List<RecruitmentSummary> items;
}
