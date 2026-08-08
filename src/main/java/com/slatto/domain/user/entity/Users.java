package com.slatto.domain.user.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.enums.SocialType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", nullable = true, length = 500)
    private String profileImageUrl;

    @Column(name = "bio", nullable = true, columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    private SocialType socialType;

    @Column(name = "social_id", nullable = true, length = 255)
    private String socialId;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    @Column(name = "term", nullable = false)
    private Boolean term;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted;

    private Users(String email, String nickname, String profileImageUrl, SocialType socialType, String socialId) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.socialType = socialType;
        this.socialId = socialId;
        this.term = false;
        this.onboardingCompleted = false;
    }

    public static Users createSocialUser(
        String email,
        String nickname,
        String profileImageUrl,
        SocialType socialType,
        String socialId
    ) {
        return new Users(email, nickname, profileImageUrl, socialType, socialId);
    }

    public static Users createEmailUser(String email, String nickname, String encodedPassword) {
        Users user = new Users(email, nickname, null, SocialType.EMAIL, null);
        user.password = encodedPassword;

        return user;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 소셜로만 가입해 비밀번호가 없는 계정과 이메일 가입 계정을 구분한다.
    public boolean hasPassword() {
        return password != null;
    }

    public void linkSocialAccount(SocialType socialType, String socialId) {
        this.socialType = socialType;
        this.socialId = socialId;
    }

    public void updateProfile(String nickname, String bio, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (bio != null) {
            this.bio = bio;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    // email 과 nickname 이 NOT NULL 이라 비울 수 없다. id 를 섞은 고유 값으로 덮어
    // 개인정보를 지우면서 email 유니크 제약도 피한다. 원래 주소가 사라지므로 재가입이 열린다.
    // .invalid 는 RFC 2606 예약 TLD 라 실수로 발송돼도 외부로 나가지 않는다.
    public void withdraw(LocalDateTime withdrawnAt) {
        this.deletedAt = withdrawnAt;
        this.email = "withdrawn_" + id + "@slatto.invalid";
        this.nickname = "탈퇴한 사용자_" + id;
        this.password = null;
        this.socialId = null;
        this.profileImageUrl = null;
        this.bio = null;
    }

    public void completeOnboarding(String nickname, String bio, String profileImageUrl) {
        this.nickname = nickname;
        this.bio = bio;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        this.term = true;
        this.onboardingCompleted = true;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

}
