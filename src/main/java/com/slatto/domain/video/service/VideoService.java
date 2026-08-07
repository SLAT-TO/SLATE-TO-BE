package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.video.client.YoutubeApiClient;
import com.slatto.domain.video.client.YoutubeApiClient.YoutubeVideoInfo;
import com.slatto.domain.video.dto.request.VideoRequest.VideoCreateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoBookmarkUpdateReqDTO;
import com.slatto.domain.video.dto.request.VideoRequest.VideoUpdateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoBookmarkUpdateResDTO;
import com.slatto.domain.video.dto.request.VideoRequest.YoutubeValidateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoDetailResDTO;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
    private static final String BOOKMARK_UPDATED_MESSAGE = "북마크 상태가 변경되었습니다.";

    private final VideoProjectAccessRepository projectAccessRepository;
    private final VideoRepository videoRepository;
    private final VideoBookmarkRepository videoBookmarkRepository;
    private final NotificationService notificationService;
    private final YoutubeUrlParser youtubeUrlParser;
    private final YoutubeApiClient youtubeApiClient;

    @Transactional
    public VideoDetailResDTO getVideo(Long memberId, Long projectId, Long videoId) {
        if (!projectAccessRepository.projectExistsById(projectId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Video video = videoRepository.findByIdAndProjectId(videoId, projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        boolean bookmarked = videoBookmarkRepository.findByVideoIdAndUserId(videoId, memberId).isPresent();
        List<String> projectTags = resolveProjectTags(
                video.getProject(),
                projectAccessRepository.findProjectRoleNames(projectId)
        );

        notificationService.markVideoFeedbackNotificationsAsRead(memberId, videoId);

        return VideoDetailResDTO.from(video, bookmarked, projectTags);
    }

    @Transactional
    public VideoBookmarkUpdateResDTO updateBookmark(
            Long memberId,
            Long projectId,
            Long videoId,
            VideoBookmarkUpdateReqDTO request
    ) {
        if (!projectAccessRepository.projectExistsById(projectId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
        if (!projectAccessRepository.existsByMemberIdAndProjectId(memberId, projectId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        Video video = videoRepository.findByIdAndProjectId(videoId, projectId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        if (request.bookmarked()) {
            videoBookmarkRepository.insertIgnore(video.getId(), memberId);
        } else {
            videoBookmarkRepository.deleteByVideoIdAndUserId(video.getId(), memberId);
        }

        return new VideoBookmarkUpdateResDTO(videoId, request.bookmarked(), BOOKMARK_UPDATED_MESSAGE);
    }

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
        Map<Long, Integer> unreadCommentCounts = notificationService.getUnreadVideoFeedbackCounts(
                memberId,
                videoIds
        );
        List<VideoItemResDTO> items = currentPageVideos.stream()
                .map(video -> VideoItemResDTO.from(
                        video,
                        bookmarkedVideoIds.contains(video.getId()),
                        unreadCommentCounts.getOrDefault(video.getId(), 0) > 0
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

    private List<String> resolveProjectTags(Project project, List<RoleName> roleNames) {
        return Stream.concat(
                        Stream.of(
                                resolveCategoryName(project.getType()),
                                resolveLengthType(project.getLengthType()),
                                resolveKind(project.getKind())
                        ),
                        roleNames.stream().map(this::resolveRoleName)
                )
                .filter(tag -> tag != null && !tag.isBlank())
                .distinct()
                .toList();
    }

    private String resolveCategoryName(CategoryName type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case YOUTUBE_CONTENT -> "유튜브 콘텐츠";
            case AD_BRAND -> "광고/브랜드 영상";
            case MUSIC_VIDEO -> "뮤직비디오";
            case WEDDING_EVENT -> "웨딩/이벤트 영상";
            case DOCUMENTARY -> "다큐멘터리";
            case FILM_DRAMA -> "영화/드라마";
            case CORPORATE_PROMO -> "기업 홍보 영상";
            case ETC -> "기타";
        };
    }

    private String resolveLengthType(LengthType lengthType) {
        if (lengthType == null) {
            return null;
        }
        return switch (lengthType) {
            case LONG_FORM -> "장편";
            case SHORT_FORM -> "단편";
        };
    }

    private String resolveKind(Kind kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case PERSONAL -> "개인";
            case EXTERNAL -> "외주";
        };
    }

    private String resolveRoleName(RoleName roleName) {
        return switch (roleName) {
            case DIRECTOR -> "연출";
            case PD -> "PD";
            case CINEMATOGRAPHER -> "촬영";
            case EDITOR -> "편집";
            case ART -> "미술";
            case SOUND -> "사운드";
            case WRITER -> "작가";
            case LIGHTING -> "조명";
            case ACTOR -> "배우";
            case ETC -> "기타";
        };
    }
}
