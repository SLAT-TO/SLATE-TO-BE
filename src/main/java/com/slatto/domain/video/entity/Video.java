package com.slatto.domain.video.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.video.enums.VideoProgressStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "video",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_video_project_youtube_video_id",
                columnNames = {"project_id", "youtube_video_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {

    private static final VideoProgressStatus DEFAULT_PROGRESS_STATUS = VideoProgressStatus.IN_PROGRESS;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl;

    @Column(name = "youtube_video_id", nullable = false, length = 100)
    private String youtubeVideoId;

    @Column(name = "thumbnail_url", nullable = true, length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds", nullable = true)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 50)
    private VideoProgressStatus progressStatus;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    private Video(
            Project project,
            String youtubeUrl,
            String youtubeVideoId,
            String title,
            String thumbnailUrl,
            Integer durationSeconds,
            String memo
    ) {
        this.project = project;
        this.youtubeUrl = youtubeUrl;
        this.youtubeVideoId = youtubeVideoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.progressStatus = DEFAULT_PROGRESS_STATUS;
        this.memo = memo;
    }

    public static Video create(
            Project project,
            String youtubeUrl,
            String youtubeVideoId,
            String title,
            String thumbnailUrl,
            Integer durationSeconds,
            String memo
    ) {
        return new Video(project, youtubeUrl, youtubeVideoId, title, thumbnailUrl, durationSeconds, memo);
    }

    public void updateInfo(String title, String memo) {
        if (title != null) {
            this.title = title;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }
}
