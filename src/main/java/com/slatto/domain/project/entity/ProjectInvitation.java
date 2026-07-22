package com.slatto.domain.project.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.enums.InvitationStatus;
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
public class ProjectInvitation extends BaseEntity {

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

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "expired_at", nullable = true)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at", nullable = true)
    private LocalDateTime acceptedAt;

    private ProjectInvitation(
        Project project,
        Users inviter,
        String tokenHash,
        LocalDateTime expiresAt
    ) {
        this.project = project;
        this.inviter = inviter;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static ProjectInvitation create(
        Project project,
        Users inviter,
        String tokenHash,
        LocalDateTime expiresAt
    ) {
        return new ProjectInvitation(project, inviter, tokenHash, expiresAt);
    }

    public void accept(Users accepter) {
        this.accepter = accepter;
        this.acceptedAt = LocalDateTime.now();
    }

    public InvitationStatus getStatus() {
        if (isAccepted()) {
            return InvitationStatus.ACCEPTED;
        }

        if (isExpired()) {
            return InvitationStatus.EXPIRED;
        }

        return InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }
}
