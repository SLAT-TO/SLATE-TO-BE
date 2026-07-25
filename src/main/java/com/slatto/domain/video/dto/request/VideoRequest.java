package com.slatto.domain.video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VideoRequest {

    @Schema(description = "영상 북마크 상태 변경 요청")
    public record VideoBookmarkUpdateReqDTO(
            @NotNull(message = "북마크 상태는 필수입니다.")
            @Schema(description = "변경할 북마크 상태", example = "true")
            Boolean bookmarked
    ) {
    }

    @Schema(description = "영상 수정 요청")
    public record VideoUpdateReqDTO(
            @Pattern(regexp = "(?s).*\\S.*", message = "영상 제목은 공백일 수 없습니다.")
            @Size(max = 255, message = "영상 제목은 최대 255자까지 입력할 수 있습니다.")
            @Schema(example = "수정된 영상 제목", nullable = true)
            String title,

            @Schema(example = "수정된 영상 메모", nullable = true)
            String memo
    ) {
    }

    @Schema(description = "YouTube URL 검증 요청")
    public record YoutubeValidateReqDTO(
            @NotBlank(message = "YouTube URL은 필수입니다.")
            @Size(max = 500, message = "YouTube URL은 최대 500자까지 입력할 수 있습니다.")
            @Schema(description = "검증할 YouTube 영상 URL", example = "https://www.youtube.com/watch?v=abc123")
            String youtubeUrl,

            @NotNull(message = "프로젝트 ID는 필수입니다.")
            @Positive(message = "프로젝트 ID는 양수여야 합니다.")
            @Schema(description = "영상 등록 여부를 확인할 프로젝트 ID", example = "10")
            Long projectId
    ) {
    }

    @Schema(description = "영상 등록 요청")
    public record VideoCreateReqDTO(
            @NotBlank(message = "YouTube URL은 필수입니다.")
            @Size(max = 500, message = "YouTube URL은 최대 500자까지 입력할 수 있습니다.")
            @Schema(description = "등록할 YouTube 영상 URL", example = "https://www.youtube.com/watch?v=abc123")
            String youtubeUrl,

            @NotBlank(message = "영상 제목은 필수입니다.")
            @Size(max = 255, message = "영상 제목은 최대 255자까지 입력할 수 있습니다.")
            @Schema(description = "프로젝트에 표시할 영상 제목", example = "촬영 콘셉트 참고 영상")
            String title,

            @Schema(description = "영상 관련 메모", example = "오프닝 연출 참고", nullable = true)
            String memo
    ) {
    }
}
