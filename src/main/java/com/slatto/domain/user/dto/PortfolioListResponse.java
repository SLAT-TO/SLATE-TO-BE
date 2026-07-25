package com.slatto.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PortfolioListResponse {

    private List<PortfolioSummaryResponse> items;

    private Long nextCursor;

    private Boolean hasNext;
}
