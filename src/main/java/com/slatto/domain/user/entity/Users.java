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

    public void linkSocialAccount(SocialType socialType, String socialId) {
        this.socialType = socialType;
        this.socialId = socialId;
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

}