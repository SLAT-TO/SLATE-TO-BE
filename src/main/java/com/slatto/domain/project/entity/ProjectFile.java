package com.slatto.domain.project.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Users uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "pinned_at", nullable = true)
    private LocalDateTime pinnedAt;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private ProjectFile(
        Project project,
        Users uploader,
        String fileName,
        String contentType,
        Long fileSize,
        String description,
        Boolean isFinal,
        String storageKey
    ) {
        this.project = project;
        this.uploader = uploader;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.description = description;
        this.isFinal = Boolean.TRUE.equals(isFinal);
        this.storageKey = storageKey;
    }

    public static ProjectFile create(
        Project project,
        Users uploader,
        String fileName,
        String contentType,
        Long fileSize,
        String description,
        Boolean isFinal,
        String storageKey
    ) {
        return new ProjectFile(
            project,
            uploader,
            fileName,
            contentType,
            fileSize,
            description,
            isFinal,
            storageKey
        );
    }

    public boolean isUploadedBy(Long userId) {
        return uploader.getId().equals(userId);
    }

    public boolean isPinned() {
        return pinnedAt != null;
    }

    public void updateFileName(String fileName) {
        this.fileName = fileName;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void changeFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void pin() {
        if (pinnedAt == null) {
            this.pinnedAt = LocalDateTime.now();
        }
    }

    public void unpin() {
        this.pinnedAt = null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
