package com.slatto.domain.recruitment.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruitment_application_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplicationFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    // 업로드 시점에는 아직 지원 행이 없어 null 이다. 지원이 성공한 뒤 linkTo 로 연결된다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = true)
    private RecruitmentApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Users uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private RecruitmentApplicationFile(
        Recruitment recruitment,
        Users uploader,
        String fileName,
        String contentType,
        Long fileSize,
        String storageKey
    ) {
        this.recruitment = recruitment;
        this.uploader = uploader;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageKey = storageKey;
    }

    public static RecruitmentApplicationFile create(
        Recruitment recruitment,
        Users uploader,
        String fileName,
        String contentType,
        Long fileSize,
        String storageKey
    ) {
        return new RecruitmentApplicationFile(
            recruitment,
            uploader,
            fileName,
            contentType,
            fileSize,
            storageKey
        );
    }

    public void linkTo(RecruitmentApplication application) {
        this.application = application;
    }

    public boolean isLinked() {
        return application != null;
    }

    public boolean isUploadedBy(Long userId) {
        return uploader.getId().equals(userId);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
