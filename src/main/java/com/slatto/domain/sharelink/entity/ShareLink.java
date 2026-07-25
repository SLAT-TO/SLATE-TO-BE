package com.slatto.domain.sharelink.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseEntity {

    private static final boolean DEFAULT_ACTIVE = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "token", nullable = false, length = 255, unique = true)
    private String token;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expired_at", nullable = true)
    private LocalDateTime expiredAt;

    private ShareLink(Video video, String token, LocalDateTime expiredAt) {
        this.video = video;
        this.token = token;
        this.isActive = DEFAULT_ACTIVE;
        this.expiredAt = expiredAt;
    }

    // 토큰은 엔티티가 직접 생성 — 서비스에서 실수로 예측 가능한 값을 넣는 걸 막음
    public static ShareLink create(Video video, LocalDateTime expiredAt) {
        return new ShareLink(video, UUID.randomUUID().toString(), expiredAt);
    }

    // 활성/비활성 토글 — 현재 상태를 뒤집고 결과를 반환
    public boolean toggleActive() {
        this.isActive = !this.isActive;
        return this.isActive;
    }

    // 게스트가 실제로 들어올 수 있는 링크인지
    public boolean isUsable() {
        if (!this.isActive) return false;
        return this.expiredAt == null || this.expiredAt.isAfter(LocalDateTime.now());
    }
}