package com.slatto.domain.project.entity;

import com.slatto.domain.project.enums.ExpirationPeriod;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private Users inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepter_id", nullable = true)
    private Users accepter;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "expired_at", nullable = false)
    private ExpirationPeriod expiredAt;             // 확인 필요!

    @Column(name = "accepted_at", nullable = true)
    private LocalDateTime acceptedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
