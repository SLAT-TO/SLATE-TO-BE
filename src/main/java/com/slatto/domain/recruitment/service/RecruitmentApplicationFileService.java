package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.dto.RecruitmentApplicationFileDownloadResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationFileResponse;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.entity.RecruitmentApplicationFile;
import com.slatto.domain.recruitment.exception.RecruitmentErrorCode;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationFileRepository;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationFileService {

    private static final int MAX_FILE_COUNT = 10;
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
    private static final String STORAGE_KEY_FORMAT = "recruitments/%d/applications/%s.%s";
    // zip 은 브라우저와 OS 조합에 따라 MIME 이 갈려 둘 다 받는다.
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
        "application/pdf", Set.of("pdf"),
        "image/jpeg", Set.of("jpg", "jpeg"),
        "image/png", Set.of("png"),
        "image/webp", Set.of("webp"),
        "application/zip", Set.of("zip"),
        "application/x-zip-compressed", Set.of("zip"),
        "video/mp4", Set.of("mp4")
    );

    private final RecruitmentApplicationFileRepository recruitmentApplicationFileRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    // 지원 API 는 마감·중복 지원으로 실패할 수 있어 파일 업로드를 분리한다.
    // 여기서 올린 파일은 application_id 가 null 인 채로 남고 지원 성공 시점에 연결된다.
    @Transactional
    public RecruitmentApplicationFileResponse uploadApplicationFile(
        Long currentUserId,
        Long recruitmentId,
        MultipartFile file
    ) {
        Recruitment recruitment = recruitmentRepository.findByIdAndDeletedAtIsNull(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 본인 공고에는 지원할 수 없으므로 첨부를 올릴 이유도 없다. 업로드 단계에서 끊어야
        // 지원 시점까지 쓸모없는 S3 객체가 남지 않는다.
        if (recruitment.isWriter(currentUserId)) {
            throw new BaseException(RecruitmentErrorCode.RECRUITMENT_SELF_APPLICATION);
        }

        Users uploader = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        String fileName = resolveFileName(file);
        validateFile(file, fileName);

        String storageKey = createStorageKey(recruitmentId, fileName);
        storageService.upload(file, storageKey);
        registerStorageCleanupOnRollback(storageKey);

        RecruitmentApplicationFile saved = recruitmentApplicationFileRepository.save(
            RecruitmentApplicationFile.create(
                recruitment,
                uploader,
                fileName,
                file.getContentType(),
                file.getSize(),
                storageKey
            )
        );

        return toResponse(saved);
    }

    // 지원 저장 직후 호출된다. 소유자·대상 공고·미연결 조건은 쿼리가 걸고,
    // 조회된 건수가 요청 개수와 다르면 남의 파일이거나 이미 다른 지원에 쓰인 파일이다.
    @Transactional
    public List<RecruitmentApplicationFile> linkFilesToApplication(
        Long currentUserId,
        Long recruitmentId,
        RecruitmentApplication application,
        List<Long> fileIds
    ) {
        List<Long> requestedIds = normalizeFileIds(fileIds);
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        if (requestedIds.size() > MAX_FILE_COUNT) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_LIMIT_EXCEEDED);
        }

        List<RecruitmentApplicationFile> files = recruitmentApplicationFileRepository.findLinkableFiles(
            requestedIds,
            recruitmentId,
            currentUserId
        );

        if (files.size() != requestedIds.size()) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_NOT_LINKABLE);
        }

        files.forEach(file -> file.linkTo(application));

        return files;
    }

    public RecruitmentApplicationFileDownloadResponse downloadApplicationFile(
        Long currentUserId,
        Long recruitmentId,
        Long applicationId,
        Long fileId
    ) {
        Recruitment recruitment = recruitmentRepository.findByIdAndDeletedAtIsNull(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // recruitmentId 조건이 없으면 다른 공고의 applicationId 로 권한 검사를 우회할 수 있다.
        RecruitmentApplication application = recruitmentApplicationRepository
            .findByIdAndRecruitmentIdAndDeletedAtIsNull(applicationId, recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateFileAccess(recruitment, application, currentUserId);

        RecruitmentApplicationFile file = recruitmentApplicationFileRepository
            .findActiveFileByApplicationIdAndFileId(applicationId, fileId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        return RecruitmentApplicationFileDownloadResponse.builder()
            .fileName(file.getFileName())
            .contentType(file.getContentType())
            .fileSize(file.getFileSize())
            .inputStream(storageService.download(file.getStorageKey()))
            .build();
    }

    public List<RecruitmentApplicationFileResponse> getFileResponses(Collection<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return List.of();
        }

        return recruitmentApplicationFileRepository.findByApplicationIds(applicationIds).stream()
            .map(this::toResponse)
            .toList();
    }

    // 지원 서류는 이력서 성격이라 공고 작성자와 지원 본인에게만 연다.
    private void validateFileAccess(
        Recruitment recruitment,
        RecruitmentApplication application,
        Long currentUserId
    ) {
        if (recruitment.isWriter(currentUserId)) {
            return;
        }

        if (application.getUser().getId().equals(currentUserId)) {
            return;
        }

        throw new BaseException(RecruitmentErrorCode.APPLICATION_ACCESS_DENIED);
    }

    private List<Long> normalizeFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }

        // 같은 id 를 두 번 보내면 조회 결과는 1건이라 개수 비교가 어긋난다. 미리 중복을 제거한다.
        return fileIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private void validateFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_SIZE_EXCEEDED);
        }

        if (!isAllowedFileType(file.getContentType(), getExtension(fileName))) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_INVALID_TYPE);
        }
    }

    // MIME 과 확장자가 모두 일치해야 통과시킨다. 한쪽만 보면 확장자만 바꾼 실행 파일이 들어온다.
    private boolean isAllowedFileType(String contentType, String extension) {
        if (!StringUtils.hasText(contentType) || !StringUtils.hasText(extension)) {
            return false;
        }

        return ALLOWED_EXTENSIONS_BY_CONTENT_TYPE
            .getOrDefault(contentType.toLowerCase(Locale.ROOT), Set.of())
            .contains(extension);
    }

    private String resolveFileName(MultipartFile file) {
        if (file == null) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_EMPTY);
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_FILE_EMPTY);
        }

        // 파일명은 그대로 저장하지 않는다. 경로 구분자가 섞이면 다운로드 헤더가 깨진다.
        return StringUtils.getFilename(originalFilename);
    }

    private String createStorageKey(Long recruitmentId, String fileName) {
        return STORAGE_KEY_FORMAT.formatted(recruitmentId, UUID.randomUUID(), getExtension(fileName));
    }

    private String getExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return "";
        }

        return extension.toLowerCase(Locale.ROOT);
    }

    // 트랜잭션이 롤백되면 DB 행은 사라지지만 S3 객체는 남는다. 아무도 참조하지 않는 객체가 되므로 지운다.
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
            log.warn(
                "[Recruitment] 지원 첨부 파일 롤백 후 S3 객체 정리 실패. storageKey={}",
                storageKey,
                exception
            );
        }
    }

    private RecruitmentApplicationFileResponse toResponse(RecruitmentApplicationFile file) {
        return RecruitmentApplicationFileResponse.builder()
            .id(file.getId())
            .fileName(file.getFileName())
            .contentType(file.getContentType())
            .fileSize(file.getFileSize())
            .createdAt(file.getCreatedAt())
            .build();
    }
}
