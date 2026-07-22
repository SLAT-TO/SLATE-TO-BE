package com.slatto.domain.project.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Users writer;

    @Column(name = "title", nullable = true, length = 255)
    private String title;

    @Column(name = "content", nullable = true, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private ProjectNotice(Project project, Users writer, String title, String content) {
        this.project = project;
        this.writer = writer;
        this.title = title;
        this.content = content;
    }

    public static ProjectNotice create(Project project, Users writer, String title, String content) {
        return new ProjectNotice(project, writer, title, content);
    }

    public boolean isWrittenBy(Long userId) {
        return writer.getId().equals(userId);
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
