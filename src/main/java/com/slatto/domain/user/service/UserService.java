package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserMeResponse;
import com.slatto.domain.user.entity.Location;
import com.slatto.domain.user.entity.UserCategory;
import com.slatto.domain.user.entity.UserRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
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

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }
}
