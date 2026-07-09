package com.slatto.domain.sharelink.entity;

import com.slatto.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink {

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}