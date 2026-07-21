package com.slatto.domain.feedback.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "guest_id", nullable = true)
    private Long guestId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "feedback_type", nullable = true, length = 50)
    private String feedbackType;

    @Column(name = "status", nullable = true)
    private Boolean status;

    @Column(name = "start_time", nullable = true)
    private Long startTime;

    @Column(name = "end_time", nullable = true)
    private Long endTime;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}