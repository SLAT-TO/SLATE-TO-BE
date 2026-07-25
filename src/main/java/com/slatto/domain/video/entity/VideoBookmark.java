package com.slatto.domain.video.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "video_bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_video_bookmark_video_user",
                columnNames = {"video_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoBookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private VideoBookmark(Video video, Users user) {
        this.video = video;
        this.user = user;
    }

    public static VideoBookmark create(Video video, Users user) {
        return new VideoBookmark(video, user);
    }
}
