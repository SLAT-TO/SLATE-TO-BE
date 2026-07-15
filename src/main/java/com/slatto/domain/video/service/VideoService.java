package com.slatto.domain.video.service;

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

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long INITIAL_CURSOR = Long.MAX_VALUE;

    private final VideoProjectAccessRepository projectAccessRepository;
    private final VideoRepository videoRepository;
    private final VideoBookmarkRepository videoBookmarkRepository;

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
}
