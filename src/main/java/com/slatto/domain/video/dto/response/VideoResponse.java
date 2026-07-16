package com.slatto.domain.video.dto.response;

import com.slatto.domain.video.entity.Video;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class VideoResponse {

    @Schema(description = "YouTube URL 검증 응답")
    public record YoutubeValidateResDTO(
            @Schema(example = "true") boolean valid,
            @Schema(example = "abc123") String youtubeVideoId,
            @Schema(example = "영상 제목") String title,
            @Schema(example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg") String thumbnailUrl,
            @Schema(example = "1018") int durationSeconds,
            @Schema(example = "true") boolean playable,
            @Schema(example = "등록 가능한 영상입니다.") String message
    ) {
    }

    @Schema(description = "영상 등록 응답")
    public record VideoCreateResDTO(
            @Schema(example = "1") Long videoId,
            @Schema(example = "프로젝트 명") String title,
            @Schema(example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg") String thumbnailUrl,
            @Schema(example = "false") boolean bookmarked,
            @Schema(example = "IN_PROGRESS") String progressStatus,
            LocalDateTime createdAt
    ) {
        public static VideoCreateResDTO from(Video video) {
            return new VideoCreateResDTO(
                    video.getId(), video.getTitle(), video.getThumbnailUrl(), false,
                    video.getProgressStatus().name(), video.getCreatedAt()
            );
        }
    }

    @Schema(description = "영상 목록 조회 응답")
    public record VideoListResDTO(
            List<VideoItemResDTO> items,
            @Schema(description = "다음 페이지 조회 커서", example = "9", nullable = true)
            Long nextCursor,
            @Schema(description = "다음 목록 존재 여부", example = "true")
            boolean hasNext
    ) {
    }

    public record VideoItemResDTO(
            @Schema(example = "10") Long videoId,
            @Schema(example = "프로젝트 명") String title,
            @Schema(example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg") String thumbnailUrl,
            @Schema(example = "true") boolean bookmarked,
            @Schema(example = "IN_PROGRESS") String progressStatus,
            @Schema(example = "3") int unreadCommentCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static VideoItemResDTO from(Video video, boolean bookmarked) {
            return new VideoItemResDTO(
                    video.getId(), video.getTitle(), video.getThumbnailUrl(), bookmarked,
                    video.getProgressStatus().name(), 0, video.getCreatedAt(), video.getUpdatedAt()
            );
        }
    }
}
