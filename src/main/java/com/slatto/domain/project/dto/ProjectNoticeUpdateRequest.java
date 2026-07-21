package com.slatto.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectNoticeUpdateRequest {

    @NotBlank(message = "공지 제목은 필수입니다.")
    @Size(max = 100, message = "공지 제목은 최대 100자까지 입력할 수 있습니다.")
    private String title;

    @NotBlank(message = "공지 내용은 필수입니다.")
    private String content;
}
