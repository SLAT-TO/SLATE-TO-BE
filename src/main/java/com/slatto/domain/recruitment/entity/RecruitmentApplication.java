package com.slatto.domain.recruitment.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruitment_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @Column(name = "message", nullable = true, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = true, length = 50)
    private String status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}