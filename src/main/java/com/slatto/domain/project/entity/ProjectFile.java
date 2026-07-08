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

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = true)
    private Long fileSize;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Column(name = "pinned_at", nullable = true)
    private LocalDateTime pinnedAt;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
