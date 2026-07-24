package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final LocationRepository locationRepository;

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

        RegionName region = locationRepository.findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .map(Location::getRegionName)
            .orElse(null);

        return UserMeResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .region(region)
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

        locationRepository.save(Location.createUserLocation(user, request.getRegion()));

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

        if (request.getLocation() != null) {
            locationRepository.findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
                .ifPresentOrElse(
                    location -> location.changeRegion(request.getLocation()),
                    () -> locationRepository.save(Location.createUserLocation(user, request.getLocation()))
                );
        }

        boolean associationChanged = request.getRoles() != null
            || request.getCategories() != null
            || request.getLocation() != null;
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

        RegionName region = locationRepository.findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .map(Location::getRegionName)
            .orElse(null);

        return UserProfileUpdateResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .location(region)
            .primaryRole(roles.isEmpty() ? null : roles.get(0))
            .roles(roles)
            .categories(categories)
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

        RegionName region = locationRepository.findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .map(Location::getRegionName)
            .orElse(null);

        return UserPublicProfileResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .location(region)
            .primaryRole(roles.isEmpty() ? null : roles.get(0))
            .roles(roles)
            .categories(categories)
            .stats(UserPublicProfileResponse.Stats.empty())
            .build();
    }

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }
}
