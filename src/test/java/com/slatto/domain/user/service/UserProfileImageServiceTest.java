package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.UserProfileImageResponse;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.LocationRepository;
import com.slatto.domain.user.repository.UserCategoryRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.repository.UserRoleRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileImageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "publicBaseUrl", "https://cdn.slatto.cloud");
    }

    @Test
    void PNG_프로필_이미지를_사용자별_S3_경로에_업로드하고_CDN_URL을_반환한다() {
        // 프로필 이미지는 프로젝트 파일 경로와 분리하고, FE에는 storageKey 대신 바로 사용할 CDN URL을 내려준다.
        Users user = user(1L, null);
        MockMultipartFile file = new MockMultipartFile(
            "file", "green.png", "image/png", "profile-image".getBytes()
        );
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        UserProfileImageResponse response = userService.uploadProfileImage(1L, file);

        ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(eq(file), storageKeyCaptor.capture());
        assertThat(storageKeyCaptor.getValue()).matches("users/1/profile-images/[a-f0-9-]+\\.png");
        assertThat(response.getProfileImageUrl())
            .isEqualTo("https://cdn.slatto.cloud/" + storageKeyCaptor.getValue());
        assertThat(user.getProfileImageUrl()).isEqualTo(response.getProfileImageUrl());
    }

    @Test
    void 허용하지_않은_이미지_형식은_S3에_업로드하지_않는다() {
        // MIME 타입과 확장자가 허용 목록에 없으면 저장 전 요청을 차단해야 한다.
        Users user = user(1L, null);
        MockMultipartFile file = new MockMultipartFile(
            "file", "green.gif", "image/gif", "profile-image".getBytes()
        );
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.uploadProfileImage(1L, file))
            .isInstanceOf(BaseException.class);

        verify(storageService, never()).upload(any(), anyString());
    }

    private Users user(Long id, String profileImageUrl) {
        Users user = Users.createSocialUser(
            "green@example.com", "그린", profileImageUrl, SocialType.GOOGLE, "google-green"
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
