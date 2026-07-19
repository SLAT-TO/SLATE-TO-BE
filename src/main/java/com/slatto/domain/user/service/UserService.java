package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.dto.UserOnboardingRequest;
import com.slatto.domain.user.dto.UserOnboardingResponse;
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

        if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
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

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }
}
