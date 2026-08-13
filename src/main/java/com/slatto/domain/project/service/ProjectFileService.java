package com.slatto.domain.project.service;

import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.domain.project.dto.ProjectFileDownloadResponse;
import com.slatto.domain.project.dto.ProjectFileResponse;
import com.slatto.domain.project.dto.ProjectFileListResponse;
import com.slatto.domain.project.dto.ProjectFilePinResponse;
import com.slatto.domain.project.dto.ProjectFileUpdateRequest;
import com.slatto.domain.project.dto.ProjectFileUploadRequest;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectFileRepository;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectFileService {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final String STORAGE_KEY_FORMAT = "projects/%d/files/%s.%s";
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
        "application/pdf", Set.of("pdf"),
        "image/jpeg", Set.of("jpg", "jpeg"),
        "image/png", Set.of("png"),
        "application/msword", Set.of("doc"),
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")
    );
    private static final Set<String> ALLOWED_EXTENSIONS = ALLOWED_EXTENSIONS_BY_CONTENT_TYPE.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toUnmodifiableSet());

    private final ProjectFileRepository projectFileRepository;
    private final ProjectAccessValidator projectAccessValidator;
    private final ProjectMemberRepository projectMemberRepository;
    private final StorageService storageService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public ProjectFileListResponse getProjectFiles(
        Long projectId,
        Long currentUserId,
        String keyword,
        Long cursor,
        int size
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        int pageSize = normalizePageSize(size);
        LocalDateTime cursorPinnedAt = getFileCursorPinnedAt(projectId, cursor);
        List<ProjectFile> projectFiles = projectFileRepository.findActiveFilesByCursor(
            projectId,
            keyword,
            cursor,
            cursorPinnedAt,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = projectFiles.size() > pageSize;
        List<ProjectFileResponse> items = projectFiles.stream()
            .limit(pageSize)
            .map(this::toResponse)
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).getId()
            : null;

        return ProjectFileListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    @Transactional
    public ProjectFileResponse uploadProjectFile(
        Long projectId,
        Long currentUserId,
        ProjectFileUploadRequest request,
        MultipartFile file
    ) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);

        validateFile(file);

        // 확장자는 올라온 파일에서 정한다. 사용자가 적은 이름은 화면에 보일 이름일 뿐이라
        // 확장자가 빠졌거나 실제 파일과 달라도 업로드를 막을 이유가 없다.
        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        validateFileType(contentType, extension);

        String fileName = applyExtension(request.getFileName(), extension);
        String storageKey = createStorageKey(projectId, extension);
        storageService.upload(file, storageKey);
        registerStorageCleanupOnRollback(storageKey);

        ProjectFile projectFile = ProjectFile.create(
            project,
            currentMember.getUser(),
            fileName,
            contentType,
            file.getSize(),
            request.getDescription(),
            request.getIsFinal(),
            storageKey
        );

        ProjectFile savedFile = projectFileRepository.save(projectFile);
        activityLogService.createFileUploadedLog(projectId, currentUserId, savedFile.getId(), savedFile.getFileName());
        notificationService.createFileUploadedNotifications(
            projectId,
            savedFile.getId(),
            project.getTitle(),
            savedFile.getFileName(),
            currentMember.getUser().getNickname(),
            getActiveProjectMemberUserIds(projectId),
            currentUserId
        );

        return toResponse(savedFile);
    }

    private void registerStorageCleanupOnRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }

                deleteUploadedFileQuietly(storageKey);
            }
        });
    }

    private void deleteUploadedFileQuietly(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to clean up S3 object after project file upload rollback. storageKey={}", storageKey, exception);
        }
    }

    @Transactional
    public ProjectFileResponse updateProjectFile(
        Long projectId,
        Long fileId,
        Long currentUserId,
        ProjectFileUpdateRequest request
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectFile projectFile = getActiveFileOrThrow(projectId, fileId);

        validateFileEditable(projectFile, currentMember, currentUserId);
        updateProjectFileInfo(projectFile, request);

        return toResponse(projectFile);
    }

    @Transactional
    public ProjectFilePinResponse pinProjectFile(
        Long projectId,
        Long fileId,
        Long currentUserId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectFile projectFile = getActiveFileOrThrow(projectId, fileId);

        validateFileEditable(projectFile, currentMember, currentUserId);
        projectFile.pin();

        return toPinResponse(projectFile);
    }

    @Transactional
    public ProjectFilePinResponse unpinProjectFile(
        Long projectId,
        Long fileId,
        Long currentUserId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectFile projectFile = getActiveFileOrThrow(projectId, fileId);

        validateFileEditable(projectFile, currentMember, currentUserId);
        projectFile.unpin();

        return toPinResponse(projectFile);
    }

    @Transactional
    public void deleteProjectFile(Long projectId, Long fileId, Long currentUserId) {
        projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember currentMember = projectAccessValidator.getCurrentMemberOrThrow(projectId, currentUserId);
        ProjectFile projectFile = getActiveFileOrThrow(projectId, fileId);

        validateFileEditable(projectFile, currentMember, currentUserId);

        projectFile.delete();
    }

    public ProjectFileDownloadResponse downloadProjectFile(
        Long projectId,
        Long fileId,
        Long currentUserId
    ) {
        projectAccessValidator.getProjectOrThrow(projectId);
        projectAccessValidator.validateProjectAccess(projectId, currentUserId);

        ProjectFile projectFile = getActiveFileOrThrow(projectId, fileId);

        return ProjectFileDownloadResponse.builder()
            .fileName(projectFile.getFileName())
            .contentType(projectFile.getContentType())
            .fileSize(projectFile.getFileSize())
            .inputStream(storageService.download(projectFile.getStorageKey()))
            .build();
    }

    private void updateProjectFileInfo(ProjectFile projectFile, ProjectFileUpdateRequest request) {
        if (request.getFileName() != null) {
            if (!StringUtils.hasText(request.getFileName())) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }

            // 저장된 파일은 그대로 두고 이름만 바꾸는 것이므로 확장자도 기존 것을 유지한다.
            String extension = getExtension(projectFile.getFileName());
            projectFile.updateFileName(applyExtension(request.getFileName(), extension));
        }

        if (request.getDescription() != null) {
            projectFile.updateDescription(request.getDescription());
        }

        if (request.getIsFinal() != null) {
            projectFile.changeFinal(request.getIsFinal());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_SIZE_EXCEEDED);
        }
    }

    private void validateFileType(String contentType, String extension) {
        if (!isAllowedFileType(contentType, extension)) {
            throw new BaseException(ProjectErrorCode.PROJECT_FILE_INVALID_TYPE);
        }
    }

    /**
     * 표시할 이름 뒤에 실제 파일의 확장자를 붙인다.
     *
     * <p>사용자가 이미 같은 확장자를 적었으면 그대로 두고, 다른 확장자를 적었으면
     * 실제 파일 쪽을 따른다. 확장자가 아닌 점(예: "콘티 v1.2")은 이름의 일부로 남긴다.
     */
    private String applyExtension(String fileName, String extension) {
        if (!StringUtils.hasText(extension)) {
            return fileName;
        }

        String baseName = ALLOWED_EXTENSIONS.contains(getExtension(fileName))
            ? StringUtils.stripFilenameExtension(fileName)
            : fileName;

        // 요청 이름은 255자까지 허용하므로 확장자를 붙이면 컬럼 길이를 넘을 수 있다.
        int maxBaseLength = MAX_FILE_NAME_LENGTH - extension.length() - 1;
        if (baseName.length() > maxBaseLength) {
            baseName = baseName.substring(0, maxBaseLength);
        }

        return baseName + "." + extension;
    }

    private boolean isAllowedFileType(String contentType, String extension) {
        if (!StringUtils.hasText(contentType) || !StringUtils.hasText(extension)) {
            return false;
        }

        return ALLOWED_EXTENSIONS_BY_CONTENT_TYPE
            .getOrDefault(contentType.toLowerCase(Locale.ROOT), Set.of())
            .contains(extension);
    }

    private String createStorageKey(Long projectId, String extension) {
        return STORAGE_KEY_FORMAT.formatted(projectId, UUID.randomUUID(), extension);
    }

    private String getExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return "";
        }

        return extension.toLowerCase(Locale.ROOT);
    }

    private ProjectFile getActiveFileOrThrow(Long projectId, Long fileId) {
        return projectFileRepository.findActiveFileByProjectIdAndFileId(projectId, fileId)
            .orElseThrow(() -> new BaseException(ProjectErrorCode.PROJECT_FILE_NOT_FOUND));
    }

    private void validateFileEditable(
        ProjectFile projectFile,
        ProjectMember currentMember,
        Long currentUserId
    ) {
        if (currentMember.isAdmin() || projectFile.isUploadedBy(currentUserId)) {
            return;
        }

        throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private List<Long> getActiveProjectMemberUserIds(Long projectId) {
        return projectMemberRepository.findAllActiveMembersByProjectId(projectId)
            .stream()
            .map(ProjectMember::getUser)
            .map(Users::getId)
            .toList();
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

    private LocalDateTime getFileCursorPinnedAt(Long projectId, Long cursor) {
        if (cursor == null) {
            return null;
        }

        return getActiveFileOrThrow(projectId, cursor).getPinnedAt();
    }

    private ProjectFilePinResponse toPinResponse(ProjectFile projectFile) {
        return ProjectFilePinResponse.builder()
            .id(projectFile.getId())
            .isPinned(projectFile.isPinned())
            .pinnedAt(projectFile.getPinnedAt())
            .build();
    }
}
