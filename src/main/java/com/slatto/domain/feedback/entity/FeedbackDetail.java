package com.slatto.domain.feedback.entity;

import com.slatto.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "guest_id", nullable = true)
    private Long guestId;

    @Column(name = "content", nullable = true, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}