package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.video.dto.request.VideoRequest.VideoReferenceFileCreateReqDTO;
import com.slatto.domain.video.dto.response.VideoResponse.VideoReferenceFileCreateResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.entity.VideoReferenceFile;
import com.slatto.domain.video.repository.VideoReferenceFileRepository;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoReferenceFileService {

    private final ProjectAccessValidator projectAccessValidator;
    private final VideoRepository videoRepository;
    private final VideoReferenceFileRepository videoReferenceFileRepository;

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

    private boolean isAlreadyLinked(Long projectId, Long videoId, Long projectFileId) {
        return videoReferenceFileRepository.countActiveReferenceFile(projectId, videoId, projectFileId) > 0;
    }
}
