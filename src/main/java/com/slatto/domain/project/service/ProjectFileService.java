package com.slatto.domain.project.service;

import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectFileRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.global.exception.BaseException;
import com.slatto.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectFileService {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final String STORAGE_KEY_FORMAT = "projects/%d/files/%s.%s";
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
        "application/pdf", Set.of("pdf"),
        "image/jpeg", Set.of("jpg", "jpeg"),
        "image/png", Set.of("png"),
        "application/msword", Set.of("doc"),
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")
    );

    private final ProjectFileRepository projectFileRepository;
    private final ProjectAccessValidator projectAccessValidator;
    private final StorageService storageService;

    @Transactional
    public ProjectFileResponse uploadProjectFile(
        Long projectId,
        Long currentUserId,
        ProjectFileUploadRequest request,
        MultipartFile file
    ) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        validateFile(file, request.getFileName());

        String contentType = file.getContentType();
        String storageKey = createStorageKey(projectId, request.getFileName());
        storageService.upload(file, storageKey);

        ProjectFile projectFile = ProjectFile.create(
            project,
            currentMember.getUser(),
            request.getFileName(),
            contentType,
            file.getSize(),
            request.getDescription(),
            request.getIsFinal(),
            storageKey
        );

        if (Boolean.TRUE.equals(request.getIsPinned())) {
            projectFile.pin();
        }

        ProjectFile savedFile = projectFileRepository.save(projectFile);

        return toResponse(savedFile);
    }

    private void validateFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        String extension = getExtension(fileName);
        if (!isAllowedFileType(contentType, extension)) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_INVALID_TYPE);
        }
    }

    private boolean isAllowedFileType(String contentType, String extension) {
        if (!StringUtils.hasText(contentType) || !StringUtils.hasText(extension)) {
            return false;
        }

        return ALLOWED_EXTENSIONS_BY_CONTENT_TYPE
            .getOrDefault(contentType.toLowerCase(Locale.ROOT), Set.of())
            .contains(extension);
    }

    private String createStorageKey(Long projectId, String fileName) {
        String extension = getExtension(fileName);

        return STORAGE_KEY_FORMAT.formatted(projectId, UUID.randomUUID(), extension);
    }

    private String getExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return "";
        }

        return extension.toLowerCase(Locale.ROOT);
    }

    private ProjectFileResponse toResponse(ProjectFile projectFile) {
        Users uploader = projectFile.getUploader();

        return ProjectFileResponse.builder()
            .id(projectFile.getId())
            .fileName(projectFile.getFileName())
            .description(projectFile.getDescription())
            .contentType(projectFile.getContentType())
            .fileSize(projectFile.getFileSize())
            .isPinned(projectFile.isPinned())
            .isFinal(projectFile.getIsFinal())
            .uploader(ProjectFileResponse.UploaderSummary.builder()
                .id(uploader.getId())
                .nickname(uploader.getNickname())
                .build())
            .createdAt(projectFile.getCreatedAt())
            .updatedAt(projectFile.getUpdatedAt())
            .build();
    }
}
