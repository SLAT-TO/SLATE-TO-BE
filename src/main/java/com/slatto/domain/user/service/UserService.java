package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
import com.slatto.domain.user.dto.UserProfileImageResponse;
import com.slatto.domain.user.dto.UserPublicProfileResponse;
import com.slatto.domain.user.entity.Location;
import com.slatto.domain.user.entity.UserCategory;
import com.slatto.domain.user.entity.UserRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.exception.UserErrorCode;
import com.slatto.domain.user.repository.LocationRepository;
import com.slatto.domain.user.repository.UserCategoryRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.repository.UserRoleRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final String PROFILE_IMAGE_STORAGE_KEY_FORMAT = "users/%d/profile-images/%s.%s";
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
        "image/jpeg", Set.of("jpg", "jpeg"),
        "image/png", Set.of("png"),
        "image/webp", Set.of("webp")
    );

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final LocationRepository locationRepository;
    private final StorageService storageService;

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    public UserMeResponse getMyInfo(Long userId) {
        Users user = getUserOrThrow(userId);

        List<RoleName> roles = userRoleRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserRole::getRoleName)
            .toList();

        List<CategoryName> categories = userCategoryRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserCategory::getCategoryName)
            .toList();

        return UserMeResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .regions(getUserRegions(userId))
            .socialType(user.getSocialType())
            .primaryRole(roles.isEmpty() ? null : roles.get(0))
            .roles(roles)
            .categories(categories)
            .onboardingCompleted(user.getOnboardingCompleted())
            .createdAt(user.getCreatedAt())
            .build();
    }

    @Transactional
    public UserOnboardingResponse completeOnboarding(Long userId, UserOnboardingRequest request) {
        Users user = getUserOrThrow(userId);

        // onboarding_completed 를 조건부 UPDATE 로 선점해 동시 요청 중 하나만 통과시킨다.
        if (userRepository.markOnboardingCompleted(userId) == 0) {
            throw new BaseException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        user.completeOnboarding(request.getNickname(), request.getBio(), request.getProfileImageUrl());

        List<UserRole> roles = request.getRoles()
            .stream()
            .distinct()
            .map(roleName -> UserRole.create(user, roleName))
            .toList();
        userRoleRepository.saveAll(roles);

        List<UserCategory> categories = request.getCategories()
            .stream()
            .distinct()
            .map(categoryName -> UserCategory.create(user, categoryName))
            .toList();
        userCategoryRepository.saveAll(categories);

        List<Location> locations = request.getRegions()
            .stream()
            .distinct()
            .map(regionName -> Location.createUserLocation(user, regionName))
            .toList();
        locationRepository.saveAll(locations);

        userRepository.flush();

        return UserOnboardingResponse.builder()
            .id(user.getId())
            .onboardingCompleted(user.getOnboardingCompleted())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    @Transactional
    public UserProfileUpdateResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        Users user = getUserOrThrow(userId);

        if (request.getNickname() != null
            && !request.getNickname().equals(user.getNickname())
            && userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(request.getNickname(), userId)) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        user.updateProfile(request.getNickname(), request.getBio(), request.getProfileImageUrl());

        if (request.getRoles() != null) {
            userRoleRepository.deleteByUserId(userId);
            userRoleRepository.flush();

            List<UserRole> roles = request.getRoles()
                .stream()
                .distinct()
                .map(roleName -> UserRole.create(user, roleName))
                .toList();
            userRoleRepository.saveAll(roles);
        }

        if (request.getCategories() != null) {
            userCategoryRepository.deleteByUserId(userId);
            userCategoryRepository.flush();

            List<UserCategory> categories = request.getCategories()
                .stream()
                .distinct()
                .map(categoryName -> UserCategory.create(user, categoryName))
                .toList();
            userCategoryRepository.saveAll(categories);
        }

        // 부분 갱신이 아니라 전체 교체다. 남은 행을 지우지 않으면 지역이 계속 누적된다.
        if (request.getLocations() != null) {
            locationRepository.deleteByUserIdAndRecruitmentIsNull(userId);
            locationRepository.flush();

            List<Location> locations = request.getLocations()
                .stream()
                .distinct()
                .map(regionName -> Location.createUserLocation(user, regionName))
                .toList();
            locationRepository.saveAll(locations);
        }

        boolean associationChanged = request.getRoles() != null
            || request.getCategories() != null
            || request.getLocations() != null;
        if (associationChanged) {
            user.markUpdated();
        }

        userRepository.flush();

        List<RoleName> roles = userRoleRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserRole::getRoleName)
            .toList();

        List<CategoryName> categories = userCategoryRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserCategory::getCategoryName)
            .toList();

        return UserProfileUpdateResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .locations(getUserRegions(userId))
            .primaryRole(roles.isEmpty() ? null : roles.get(0))
            .roles(roles)
            .categories(categories)
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    @Transactional
    public UserProfileImageResponse uploadProfileImage(Long userId, MultipartFile file) {
        Users user = getUserOrThrow(userId);
        validateProfileImage(file);

        String storageKey = createProfileImageStorageKey(userId, file.getOriginalFilename());
        String profileImageUrl = createProfileImageUrl(storageKey);
        String previousStorageKey = extractManagedStorageKey(user.getProfileImageUrl());

        try {
            storageService.upload(file, storageKey);
        } catch (RuntimeException exception) {
            deleteStorageObjectQuietly(storageKey, "profile image upload");
            throw exception;
        }
        registerUploadedFileCleanupOnRollback(storageKey);

        user.updateProfileImage(profileImageUrl);
        userRepository.flush();
        registerPreviousFileDeletionAfterCommit(previousStorageKey);

        return UserProfileImageResponse.builder()
            .profileImageUrl(profileImageUrl)
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    public UserPublicProfileResponse getPublicProfile(Long userId) {
        Users user = getUserOrThrow(userId);

        List<RoleName> roles = userRoleRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserRole::getRoleName)
            .toList();

        List<CategoryName> categories = userCategoryRepository.findAllByUserIdOrderByIdAsc(userId)
            .stream()
            .map(UserCategory::getCategoryName)
            .toList();

        return UserPublicProfileResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .locations(getUserRegions(userId))
            .primaryRole(roles.isEmpty() ? null : roles.get(0))
            .roles(roles)
            .categories(categories)
            .stats(UserPublicProfileResponse.Stats.empty())
            .build();
    }

    private List<RegionName> getUserRegions(Long userId) {
        return locationRepository.findAllByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .stream()
            .map(Location::getRegionName)
            .toList();
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(UserErrorCode.PROFILE_IMAGE_EMPTY);
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BaseException(UserErrorCode.PROFILE_IMAGE_SIZE_EXCEEDED);
        }

        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!isAllowedProfileImage(contentType, extension)) {
            throw new BaseException(UserErrorCode.PROFILE_IMAGE_INVALID_TYPE);
        }
    }

    private boolean isAllowedProfileImage(String contentType, String extension) {
        if (!StringUtils.hasText(contentType) || !StringUtils.hasText(extension)) {
            return false;
        }

        return ALLOWED_EXTENSIONS_BY_CONTENT_TYPE
            .getOrDefault(contentType.toLowerCase(Locale.ROOT), Set.of())
            .contains(extension);
    }

    private String createProfileImageStorageKey(Long userId, String originalFilename) {
        return PROFILE_IMAGE_STORAGE_KEY_FORMAT.formatted(userId, UUID.randomUUID(), getExtension(originalFilename));
    }

    private String createProfileImageUrl(String storageKey) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        return publicBaseUrl.replaceAll("/+$", "") + "/" + storageKey;
    }

    private String extractManagedStorageKey(String profileImageUrl) {
        if (!StringUtils.hasText(publicBaseUrl) || !StringUtils.hasText(profileImageUrl)) {
            return null;
        }

        String normalizedBaseUrl = publicBaseUrl.replaceAll("/+$", "") + "/";
        if (!profileImageUrl.startsWith(normalizedBaseUrl)) {
            return null;
        }

        return profileImageUrl.substring(normalizedBaseUrl.length());
    }

    private void registerUploadedFileCleanupOnRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteStorageObjectQuietly(storageKey, "profile image upload rollback");
                }
            }
        });
    }

    private void registerPreviousFileDeletionAfterCommit(String previousStorageKey) {
        if (!StringUtils.hasText(previousStorageKey) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteStorageObjectQuietly(previousStorageKey, "profile image replacement");
                }
            }
        });
    }

    private void deleteStorageObjectQuietly(String storageKey, String context) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete S3 object after {}. storageKey={}", context, storageKey, exception);
        }
    }

    private String getExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        return StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ROOT) : "";
    }

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }
}
