package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.video.dto.request.VideoRequest.VideoCreateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoItemResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoListResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.repository.VideoBookmarkRepository;
import com.slatto.domain.video.repository.VideoProjectAccessRepository;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long INITIAL_CURSOR = Long.MAX_VALUE;
    private static final String THUMBNAIL_URL_FORMAT = "https://img.youtube.com/vi/%s/maxresdefault.jpg";
    private static final Pattern YOUTUBE_PATH_PATTERN = Pattern.compile("^/(?:shorts|embed)/([^/?#]+)");

    private final VideoProjectAccessRepository projectAccessRepository;
    private final VideoRepository videoRepository;
    private final VideoBookmarkRepository videoBookmarkRepository;

    @Transactional
    public VideoCreateResDTO createVideo(Long memberId, Long projectId, VideoCreateReqDTO request) {
        Project project = projectAccessRepository.findProjectById(projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        String youtubeVideoId = extractYoutubeVideoId(request.youtubeUrl());
        String thumbnailUrl = THUMBNAIL_URL_FORMAT.formatted(youtubeVideoId);
        Video video = Video.create(
                project,
                request.youtubeUrl(),
                youtubeVideoId,
                request.title(),
                thumbnailUrl,
                request.memo()
        );

        return VideoCreateResDTO.from(videoRepository.save(video));
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
        List<VideoItemResDTO> items = videos.stream()
                .limit(size)
                .map(video -> VideoItemResDTO.from(
                        video,
                        videoBookmarkRepository.existsByVideoIdAndUserId(video.getId(), memberId)
                ))
                .toList();
        Long nextCursor = hasNext && !items.isEmpty() ? items.getLast().videoId() : null;

        return new VideoListResDTO(items, nextCursor, hasNext);
    }

    private String extractYoutubeVideoId(String youtubeUrl) {
        try {
            URI uri = new URI(youtubeUrl.trim());
            String host = uri.getHost();
            if (host == null) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }

            String normalizedHost = host.toLowerCase();
            if (normalizedHost.equals("youtu.be")) {
                return requireVideoId(uri.getPath().substring(1).split("/", 2)[0]);
            }
            if (!normalizedHost.equals("youtube.com") && !normalizedHost.equals("www.youtube.com")
                    && !normalizedHost.equals("m.youtube.com")) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }

            Matcher pathMatcher = YOUTUBE_PATH_PATTERN.matcher(uri.getPath());
            if (pathMatcher.find()) {
                return requireVideoId(pathMatcher.group(1));
            }

            if ("/watch".equals(uri.getPath()) && uri.getRawQuery() != null) {
                for (String parameter : uri.getRawQuery().split("&")) {
                    String[] keyValue = parameter.split("=", 2);
                    if (keyValue.length == 2 && "v".equals(keyValue[0])) {
                        return requireVideoId(keyValue[1]);
                    }
                }
            }
        } catch (URISyntaxException | IndexOutOfBoundsException exception) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        throw new BaseException(CommonErrorCode.BAD_REQUEST);
    }

    private String requireVideoId(String videoId) {
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{6,100}")) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        return videoId;
    }
}
