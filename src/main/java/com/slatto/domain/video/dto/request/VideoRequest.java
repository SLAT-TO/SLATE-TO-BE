package com.slatto.domain.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VideoRequest {

    @Schema(description = "영상 등록 요청")
    public record VideoCreateReqDTO(
            @NotBlank(message = "YouTube URL은 필수입니다.")
            @Size(max = 500, message = "YouTube URL은 최대 500자까지 입력할 수 있습니다.")
            @Schema(example = "https://www.youtube.com/watch?v=abc123")
            String youtubeUrl,

            @NotBlank(message = "영상 제목은 필수입니다.")
            @Size(max = 255, message = "영상 제목은 최대 255자까지 입력할 수 있습니다.")
            @Schema(example = "프로젝트 명")
            String title,

            @Schema(example = "영상에 관련된 메모", nullable = true)
            String memo
    ) {
    }
}
