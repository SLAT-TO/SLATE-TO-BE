package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.video.client.YoutubeApiClient;
import com.slatto.domain.video.client.YoutubeApiClient.YoutubeVideoInfo;
import com.slatto.domain.video.dto.request.VideoRequest.VideoCreateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoUpdateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.YoutubeValidateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoItemResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoListResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoUpdateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.YoutubeValidateResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.repository.VideoBookmarkRepository;
import com.slatto.domain.video.repository.VideoProjectAccessRepository;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long INITIAL_CURSOR = Long.MAX_VALUE;
    private static final String THUMBNAIL_URL_FORMAT = "https://img.youtube.com/vi/%s/maxresdefault.jpg";
    private static final String VALIDATION_SUCCESS_MESSAGE = "등록 가능한 영상입니다.";
    private static final String PRIVATE_VIDEO_MESSAGE = "비공개 영상은 등록할 수 없습니다.";
    private static final String NOT_EMBEDDABLE_MESSAGE = "재생할 수 없는 영상은 등록할 수 없습니다.";
    private static final String VIDEO_DELETED_MESSAGE = "영상이 삭제되었습니다.";

    private final VideoProjectAccessRepository projectAccessRepository;
    private final VideoRepository videoRepository;
    private final VideoBookmarkRepository videoBookmarkRepository;
    private final YoutubeUrlParser youtubeUrlParser;
    private final YoutubeApiClient youtubeApiClient;

    public YoutubeValidateResDTO validateYoutubeUrl(Long memberId, YoutubeValidateReqDTO request) {
        if (projectAccessRepository.findProjectById(request.projectId()).isEmpty()) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, request.projectId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        String youtubeVideoId = youtubeUrlParser.extractVideoId(request.youtubeUrl());
        if (videoRepository.existsByProjectIdAndYoutubeVideoId(request.projectId(), youtubeVideoId)) {
            throw new BaseException(CommonErrorCode.CONFLICT);
        }

        YoutubeVideoInfo videoInfo = youtubeApiClient.getVideo(youtubeVideoId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        boolean valid = !isPrivate(videoInfo.privacyStatus()) && videoInfo.embeddable();

        return new YoutubeValidateResDTO(
                valid,
                youtubeVideoId,
                videoInfo.title(),
                videoInfo.thumbnailUrl(),
                videoInfo.durationSeconds(),
                valid,
                resolveValidationMessage(videoInfo)
        );
    }

    @Transactional
    public VideoCreateResDTO createVideo(Long memberId, Long projectId, VideoCreateReqDTO request) {
        Project project = projectAccessRepository.findProjectById(projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        String youtubeVideoId = youtubeUrlParser.extractVideoId(request.youtubeUrl());
        if (videoRepository.existsByProjectIdAndYoutubeVideoId(projectId, youtubeVideoId)) {
            throw new BaseException(CommonErrorCode.CONFLICT);
        }

        YoutubeVideoInfo videoInfo = youtubeApiClient.getVideo(youtubeVideoId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        if (isPrivate(videoInfo.privacyStatus()) || !videoInfo.embeddable()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        String thumbnailUrl = videoInfo.thumbnailUrl() != null
                ? videoInfo.thumbnailUrl()
                : THUMBNAIL_URL_FORMAT.formatted(youtubeVideoId);
        Video video = Video.create(
                project,
                request.youtubeUrl(),
                youtubeVideoId,
                request.title(),
                thumbnailUrl,
                videoInfo.durationSeconds(),
                request.memo()
        );

        try {
            Video savedVideo = videoRepository.save(video);
            videoRepository.flush();
            return VideoCreateResDTO.from(savedVideo);
        } catch (DataIntegrityViolationException exception) {
            throw new BaseException(CommonErrorCode.CONFLICT);
        }
    }

    public VideoListResDTO getVideos(Long memberId, Long projectId, Long cursor, Integer requestedSize) {
        if (!projectAccessRepository.projectExistsById(projectId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        int size = requestedSize == null ? DEFAULT_SIZE : Math.min(requestedSize, MAX_SIZE);
        long cursorId = cursor == null ? INITIAL_CURSOR : cursor;
        List<Video> videos = videoRepository.findByProjectIdAndIdLessThanOrderByIdDesc(
                projectId, cursorId, size + 1
        );
        boolean hasNext = videos.size() > size;
        List<Video> currentPageVideos = videos.stream()
                .limit(size)
                .toList();
        List<Long> videoIds = currentPageVideos.stream()
                .map(Video::getId)
                .toList();
        Set<Long> bookmarkedVideoIds = videoIds.isEmpty()
                ? Set.of()
                : Set.copyOf(videoBookmarkRepository.findBookmarkedVideoIdsByUserIdAndVideoIds(memberId, videoIds));
        List<VideoItemResDTO> items = currentPageVideos.stream()
                .map(video -> VideoItemResDTO.from(
                        video,
                        bookmarkedVideoIds.contains(video.getId())
                ))
                .toList();
        Long nextCursor = hasNext && !items.isEmpty() ? items.getLast().videoId() : null;

        return new VideoListResDTO(items, nextCursor, hasNext);
    }

    @Transactional
    public VideoDeleteResDTO deleteVideo(Long memberId, Long projectId, Long videoId) {
        if (!projectAccessRepository.projectExistsById(projectId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Video video = videoRepository.findByIdAndProjectId(videoId, projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        videoRepository.delete(video);

        return new VideoDeleteResDTO(videoId, VIDEO_DELETED_MESSAGE);
    }

    @Transactional
    public VideoUpdateResDTO updateVideo(
            Long memberId,
            Long projectId,
            Long videoId,
            VideoUpdateReqDTO request
    ) {
        validateUpdateRequest(request);
        if (!projectAccessRepository.projectExistsById(projectId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Video video = videoRepository.findByIdAndProjectId(videoId, projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        video.updateInfo(request.title(), request.memo());
        videoRepository.flush();

        return VideoUpdateResDTO.from(video);
    }

    private void validateUpdateRequest(VideoUpdateReqDTO request) {
        if (request.title() == null && request.memo() == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        if (request.title() != null && request.title().isBlank()) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private boolean isPrivate(String privacyStatus) {
        return "private".equals(privacyStatus);
    }

    private String resolveValidationMessage(YoutubeVideoInfo videoInfo) {
        if (isPrivate(videoInfo.privacyStatus())) {
            return PRIVATE_VIDEO_MESSAGE;
        }
        if (!videoInfo.embeddable()) {
            return NOT_EMBEDDABLE_MESSAGE;
        }
        return VALIDATION_SUCCESS_MESSAGE;
    }
}
