package com.slatto.domain.video.dto.response;

import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.entity.VideoReferenceFile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class VideoResponse {

    @Schema(description = "영상 상세 조회 응답")
    public record VideoDetailResDTO(
            @Schema(description = "영상 ID", example = "1") Long videoId,
            @Schema(description = "프로젝트 ID", example = "10") Long projectId,
            @Schema(description = "영상 제목", example = "버전명") String title,
            @Schema(description = "YouTube 영상 URL", example = "https://www.youtube.com/watch?v=abc123")
            String youtubeUrl,
            @Schema(description = "YouTube 영상 ID", example = "abc123") String youtubeVideoId,
            @Schema(description = "영상 썸네일 URL", example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg")
            String thumbnailUrl,
            @Schema(description = "영상 진행 상태", example = "IN_PROGRESS") String progressStatus,
            @Schema(description = "로그인한 사용자의 북마크 여부", example = "true") boolean bookmarked,
            @Schema(description = "프로젝트 소개", example = "프로젝트 소개글") String description,
            @Schema(description = "영상 메모", example = "영상에 관련된 메모", nullable = true) String memo,
            @Schema(description = "프로젝트 태그", example = "[\"뮤직비디오\", \"단편\", \"외주\", \"연출\"]")
            List<String> projectTags,
            @Schema(description = "생성일", example = "2026-05-20T00:00:00") LocalDateTime createdAt,
            @Schema(description = "수정일", example = "2026-05-25T00:00:00") LocalDateTime updatedAt
    ) {
        public static VideoDetailResDTO from(
                Video video,
                boolean bookmarked,
                List<String> projectTags
        ) {
            return new VideoDetailResDTO(
                    video.getId(),
                    video.getProject().getId(),
                    video.getTitle(),
                    video.getYoutubeUrl(),
                    video.getYoutubeVideoId(),
                    video.getThumbnailUrl(),
                    video.getProgressStatus().name(),
                    bookmarked,
                    video.getProject().getDescription(),
                    video.getMemo(),
                    projectTags,
                    video.getCreatedAt(),
                    video.getUpdatedAt()
            );
        }
    }

    @Schema(description = "게스트 영상 상세 조회 응답")
    public record GuestVideoDetailResDTO(
            @Schema(description = "영상 ID", example = "1") Long videoId,
            @Schema(description = "영상 제목", example = "버전명") String title,
            @Schema(description = "YouTube 영상 URL", example = "https://www.youtube.com/watch?v=abc123")
            String youtubeUrl,
            @Schema(description = "YouTube 영상 ID", example = "abc123") String youtubeVideoId,
            @Schema(description = "영상 썸네일 URL", example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg")
            String thumbnailUrl,
            @Schema(description = "영상 진행 상태", example = "IN_PROGRESS") String progressStatus,
            @Schema(description = "프로젝트 소개", example = "프로젝트 소개글") String description,
            @Schema(description = "영상 메모", example = "영상에 관련된 메모", nullable = true) String memo,
            @Schema(description = "프로젝트 태그", example = "[\"뮤직비디오\", \"단편\", \"외주\", \"연출\"]")
            List<String> projectTags,
            @Schema(description = "생성일", example = "2026-05-20T00:00:00") LocalDateTime createdAt,
            @Schema(description = "수정일", example = "2026-05-25T00:00:00") LocalDateTime updatedAt
    ) {
        public static GuestVideoDetailResDTO from(VideoDetailResDTO detail) {
            return new GuestVideoDetailResDTO(
                    detail.videoId(),
                    detail.title(),
                    detail.youtubeUrl(),
                    detail.youtubeVideoId(),
                    detail.thumbnailUrl(),
                    detail.progressStatus(),
                    detail.description(),
                    detail.memo(),
                    detail.projectTags(),
                    detail.createdAt(),
                    detail.updatedAt()
            );
        }
    }

    @Schema(description = "영상 북마크 상태 변경 응답")
    public record VideoBookmarkUpdateResDTO(
            @Schema(description = "영상 ID", example = "1") Long videoId,
            @Schema(description = "변경된 북마크 상태", example = "true") boolean bookmarked,
            @Schema(description = "처리 결과 메시지", example = "북마크 상태가 변경되었습니다.") String message
    ) {
    }

    @Schema(description = "영상 수정 응답")
    public record VideoUpdateResDTO(
            @Schema(example = "1") Long videoId,
            @Schema(example = "수정된 영상 제목") String title,
            @Schema(example = "수정된 영상 메모", nullable = true) String memo,
            LocalDateTime updatedAt
    ) {
        public static VideoUpdateResDTO from(Video video) {
            return new VideoUpdateResDTO(
                    video.getId(), video.getTitle(), video.getMemo(), video.getUpdatedAt()
            );
        }
    }

    @Schema(description = "영상 삭제 응답")
    public record VideoDeleteResDTO(
            @Schema(example = "1") Long videoId,
            @Schema(example = "영상이 삭제되었습니다.") String message
    ) {
    }

    @Schema(description = "YouTube URL 검증 응답")
    public record YoutubeValidateResDTO(
            @Schema(description = "프로젝트에 등록 가능한지 여부", example = "true") boolean valid,
            @Schema(description = "URL에서 추출한 YouTube 영상 ID", example = "abc123") String youtubeVideoId,
            @Schema(description = "YouTube에서 조회한 영상 제목", example = "영상 제목") String title,
            @Schema(description = "YouTube 썸네일 URL", example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg")
            String thumbnailUrl,
            @Schema(description = "영상 길이(초)", example = "1018") int durationSeconds,
            @Schema(description = "외부 서비스에서 재생 가능한지 여부", example = "true") boolean playable,
            @Schema(description = "검증 결과 안내 메시지", example = "등록 가능한 영상입니다.") String message
    ) {
    }

    @Schema(description = "영상 등록 응답")
    public record VideoCreateResDTO(
            @Schema(example = "1") Long videoId,
            @Schema(example = "촬영 콘셉트 참고 영상") String title,
            @Schema(example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg") String thumbnailUrl,
            @Schema(example = "1018") Integer durationSeconds,
            @Schema(example = "false") boolean bookmarked,
            @Schema(example = "IN_PROGRESS") String progressStatus,
            LocalDateTime createdAt
    ) {
        public static VideoCreateResDTO from(Video video) {
            return new VideoCreateResDTO(
                    video.getId(), video.getTitle(), video.getThumbnailUrl(), video.getDurationSeconds(), false,
                    video.getProgressStatus().name(), video.getCreatedAt()
            );
        }
    }

    @Schema(description = "영상 목록 조회 응답")
    public record VideoListResDTO(
            @Schema(description = "조회된 영상 목록")
            List<VideoItemResDTO> items,
            @Schema(description = "다음 페이지 조회 커서", example = "9", nullable = true)
            Long nextCursor,
            @Schema(description = "다음 목록 존재 여부", example = "true")
            boolean hasNext
    ) {
    }

    @Schema(description = "영상 목록 항목")
    public record VideoItemResDTO(
            @Schema(example = "10") Long videoId,
            @Schema(example = "촬영 콘셉트 참고 영상") String title,
            @Schema(example = "https://img.youtube.com/vi/abc123/maxresdefault.jpg") String thumbnailUrl,
            @Schema(example = "true") boolean bookmarked,
            @Schema(example = "IN_PROGRESS") String progressStatus,
            @Schema(description = "읽지 않은 피드백 존재 여부", example = "true")
            boolean hasUnreadFeedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static VideoItemResDTO from(Video video, boolean bookmarked, boolean hasUnreadFeedback) {
            return new VideoItemResDTO(
                    video.getId(), video.getTitle(), video.getThumbnailUrl(), bookmarked,
                    video.getProgressStatus().name(), hasUnreadFeedback, video.getCreatedAt(), video.getUpdatedAt()
            );
        }
    }

    @Schema(description = "영상 참조 파일 목록 조회 응답")
    public record VideoReferenceFileListResDTO(
            @Schema(description = "참조 파일 목록")
            List<VideoReferenceFileItemResDTO> items,
            @Schema(description = "다음 페이지 조회 커서", example = "9", nullable = true)
            Long nextCursor,
            @Schema(description = "다음 목록 존재 여부", example = "true")
            boolean hasNext
    ) {
    }

    @Schema(description = "영상 참조 파일 목록 항목")
    public record VideoReferenceFileItemResDTO(
            @Schema(example = "1") Long referenceFileId,
            @Schema(example = "10") Long projectFileId,
            @Schema(example = "reference.pdf") String fileName,
            @Schema(example = "application/pdf") String contentType,
            @Schema(example = "1048576") Long fileSize,
            @Schema(example = "false") Boolean isFinal,
            VideoReferenceFileUploaderResDTO uploader,
            LocalDateTime createdAt
    ) {
        public static VideoReferenceFileItemResDTO from(VideoReferenceFile referenceFile) {
            ProjectFile projectFile = referenceFile.getProjectFile();

            return new VideoReferenceFileItemResDTO(
                    referenceFile.getId(),
                    projectFile.getId(),
                    projectFile.getFileName(),
                    projectFile.getContentType(),
                    projectFile.getFileSize(),
                    projectFile.getIsFinal(),
                    VideoReferenceFileUploaderResDTO.from(projectFile),
                    referenceFile.getCreatedAt()
            );
        }
    }

    @Schema(description = "영상 참조 파일 업로더 정보")
    public record VideoReferenceFileUploaderResDTO(
            @Schema(example = "1") Long id,
            @Schema(example = "그린") String nickname
    ) {
        public static VideoReferenceFileUploaderResDTO from(ProjectFile projectFile) {
            return new VideoReferenceFileUploaderResDTO(
                    projectFile.getUploader().getId(),
                    projectFile.getUploader().getNickname()
            );
        }
    }

    @Schema(description = "영상 참조 파일 연결 응답")
    public record VideoReferenceFileCreateResDTO(
            @Schema(example = "1") Long referenceFileId,
            @Schema(example = "10") Long projectFileId,
            LocalDateTime createdAt
    ) {
        public static VideoReferenceFileCreateResDTO from(VideoReferenceFile referenceFile) {
            return new VideoReferenceFileCreateResDTO(
                    referenceFile.getId(),
                    referenceFile.getProjectFile().getId(),
                    referenceFile.getCreatedAt()
            );
        }
    }

    @Schema(description = "영상 참조 파일 연결 제거 응답")
    public record VideoReferenceFileDeleteResDTO(
            @Schema(example = "1") Long referenceFileId,
            LocalDateTime deletedAt
    ) {
        public static VideoReferenceFileDeleteResDTO from(VideoReferenceFile referenceFile) {
            return new VideoReferenceFileDeleteResDTO(
                    referenceFile.getId(),
                    referenceFile.getDeletedAt()
            );
        }
    }
}
