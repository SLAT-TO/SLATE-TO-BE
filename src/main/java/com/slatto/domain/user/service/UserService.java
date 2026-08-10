package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
import com.slatto.domain.user.dto.UserProfileImageResponse;
import com.slatto.domain.user.dto.UserPublicProfileResponse;
import com.slatto.domain.user.dto.UserStatsResponse;
import com.slatto.domain.user.dto.UserWithdrawRequest;
import com.slatto.domain.auth.repository.RefreshTokenRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.repository.UserPortfolioRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final long MAX_PROFILE_IMAGE_SIZE = 2L * 1024 * 1024;
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
    private final UserPortfolioRepository userPortfolioRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
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
            .stats(buildStats(userId))
            .build();
    }

    // 포트폴리오 기준 집계다. 등록한 작업이 없으면 두 목록 모두 빈 배열로 나간다.
    public UserStatsResponse getStats(Long userId) {
        getUserOrThrow(userId);

        return buildStats(userId);
    }

    // getPublicProfile 은 이미 유저를 검증했다. 여기서 다시 조회하면 파생 쿼리라
    // 1차 캐시를 타지 않고 users 를 한 번 더 SELECT 한다.
    private UserStatsResponse buildStats(Long userId) {
        return UserStatsResponse.builder()
            .projectTypes(toProjectTypeStats(userPortfolioRepository.findProjectTypeStatRowsByUserId(userId)))
            .roles(toRoleStats(userPortfolioRepository.findRoleStatRowsByUserId(userId)))
            .build();
    }

    private List<UserStatsResponse.ProjectTypeStat> toProjectTypeStats(List<Object[]> rows) {
        return rows.stream()
            .map(row -> UserStatsResponse.ProjectTypeStat.builder()
                .type((CategoryName) row[0])
                .count((Long) row[1])
                .build())
            .toList();
    }

    private List<UserStatsResponse.RoleStat> toRoleStats(List<Object[]> rows) {
        return rows.stream()
            .map(row -> UserStatsResponse.RoleStat.builder()
                .role((RoleName) row[0])
                .count((Long) row[1])
                .build())
            .toList();
    }

    // 유저 행은 남긴다. 프로젝트·공고 등 연관 데이터가 FK 로 참조하고 있어 지우면 이력이 끊긴다.
    @Transactional
    public void withdraw(Long userId, UserWithdrawRequest request) {
        Users user = getUserOrThrow(userId);

        // 세션이 탈취된 상태에서 탈퇴까지 가능하면 계정을 통째로 지워버릴 수 있다.
        // matches 는 raw 가 null 이면 IllegalArgumentException 을 던져 500 이 나간다. 먼저 걸러낸다.
        if (user.hasPassword()
            && (request.getPassword() == null
            || !passwordEncoder.matches(request.getPassword(), user.getPassword()))) {
            throw new BaseException(UserErrorCode.WITHDRAW_PASSWORD_MISMATCH);
        }

        LocalDateTime withdrawnAt = LocalDateTime.now();

        // withdraw 가 URL 을 지우기 전에 키를 뽑아둔다. URL 만 비우면 스토리지 객체가 남아
        // 기존 공개 URL 을 아는 사람은 탈퇴 후에도 프로필 사진을 계속 볼 수 있다.
        String profileImageStorageKey = extractManagedStorageKey(user.getProfileImageUrl());

        userPortfolioRepository.softDeleteAllByUserId(userId, withdrawnAt);
        recruitmentRepository.softDeleteAllByWriterId(userId, withdrawnAt);
        refreshTokenRepository.deleteByUser(user);

        user.withdraw(withdrawnAt);

        registerPreviousFileDeletionAfterCommit(profileImageStorageKey);
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
