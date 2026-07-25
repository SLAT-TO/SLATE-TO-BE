package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.video.dto.request.VideoRequest.VideoReferenceFileCreateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileCreateResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileDeleteResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileItemResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileListResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.entity.VideoReferenceFile;
import com.slatto.domain.video.repository.VideoReferenceFileRepository;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoReferenceFileService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectAccessValidator projectAccessValidator;
    private final VideoRepository videoRepository;
    private final VideoReferenceFileRepository videoReferenceFileRepository;

    public VideoReferenceFileListResDTO getReferenceFiles(
        Long currentUserId,
        Long projectId,
        Long videoId,
        String keyword,
        Long cursor,
        Integer requestedSize
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);
        validateVideoBelongsToProject(projectId, videoId);

        int pageSize = normalizePageSize(requestedSize);
        List<VideoReferenceFile> referenceFiles = videoReferenceFileRepository.findActiveReferenceFilesByCursor(
            projectId,
            videoId,
            normalizeKeyword(keyword),
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = referenceFiles.size() > pageSize;
        List<VideoReferenceFileItemResDTO> items = referenceFiles.stream()
            .limit(pageSize)
            .map(VideoReferenceFileItemResDTO::from)
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).referenceFileId()
            : null;

        return new VideoReferenceFileListResDTO(items, nextCursor, hasNext);
    }

    @Transactional
    public VideoReferenceFileCreateResDTO createReferenceFile(
        Long currentUserId,
        Long projectId,
        Long videoId,
        VideoReferenceFileCreateReqDTO request
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        Video video = videoRepository.findByIdAndProjectId(videoId, projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        ProjectFile projectFile = videoReferenceFileRepository.findActiveProjectFile(projectId, request.projectFileId())
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        if (isAlreadyLinked(projectId, videoId, projectFile.getId())) {
            throw new BaseException(CommonErrorCode.CONFLICT);
        }

        VideoReferenceFile referenceFile = VideoReferenceFile.create(video, projectFile, currentMember.getUser());

        try {
            VideoReferenceFile savedReferenceFile = videoReferenceFileRepository.save(referenceFile);
            return VideoReferenceFileCreateResDTO.from(savedReferenceFile);
        } catch (DataIntegrityViolationException exception) {
            throw new BaseException(CommonErrorCode.CONFLICT);
        }
    }

    @Transactional
    public VideoReferenceFileDeleteResDTO deleteReferenceFile(
        Long currentUserId,
        Long projectId,
        Long videoId,
        Long referenceFileId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);
        validateVideoBelongsToProject(projectId, videoId);

        VideoReferenceFile referenceFile = videoReferenceFileRepository.findActiveReferenceFile(
                projectId,
                videoId,
                referenceFileId
            )
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        referenceFile.delete();

        return VideoReferenceFileDeleteResDTO.from(referenceFile);
    }

    private boolean isAlreadyLinked(Long projectId, Long videoId, Long projectFileId) {
        return videoReferenceFileRepository.countActiveReferenceFile(projectId, videoId, projectFileId) > 0;
    }

    private void validateVideoBelongsToProject(Long projectId, Long videoId) {
        videoRepository.findByIdAndProjectId(videoId, projectId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private int normalizePageSize(Integer requestedSize) {
        if (requestedSize == null || requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }
}
