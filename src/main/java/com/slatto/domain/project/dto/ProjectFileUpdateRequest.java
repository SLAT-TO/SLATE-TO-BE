package com.slatto.domain.project.dto;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFileUpdateRequest {

    @Size(max = 255, message = "파일명은 최대 255자까지 입력할 수 있습니다.")
    private String fileName;

    private String description;

    private Boolean isPinned;

    private Boolean isFinal;
}
