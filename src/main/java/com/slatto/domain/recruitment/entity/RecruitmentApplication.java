package com.slatto.domain.recruitment.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
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

    private static final RecruitmentApplicationStatus DEFAULT_STATUS = RecruitmentApplicationStatus.PENDING;

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

    @Column(name = "reference_link", nullable = true, length = 500)
    private String referenceLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private RecruitmentApplicationStatus status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private RecruitmentApplication(
        Users user,
        Recruitment recruitment,
        String message,
        String referenceLink
    ) {
        this.user = user;
        this.recruitment = recruitment;
        this.message = message;
        this.referenceLink = referenceLink;
        this.status = DEFAULT_STATUS;
    }

    public static RecruitmentApplication create(
        Users user,
        Recruitment recruitment,
        String message,
        String referenceLink
    ) {
        return new RecruitmentApplication(user, recruitment, message, referenceLink);
    }

    // status 컬럼이 nullable 이라 레거시 행은 null 이다. == 비교라 null 에서 false 로 떨어진다.
    public boolean isPending() {
        return this.status == RecruitmentApplicationStatus.PENDING;
    }

    // 전이 가드(PENDING 만 변경 가능)는 서비스가 isPending() 으로 건다.
    public void changeStatus(RecruitmentApplicationStatus status) {
        this.status = status;
    }
}
