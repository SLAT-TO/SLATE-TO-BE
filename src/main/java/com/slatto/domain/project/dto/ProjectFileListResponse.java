package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProjectFileListResponse {

    private List<ProjectFileResponse> items;

    private Long nextCursor;

    private Boolean hasNext;
}
