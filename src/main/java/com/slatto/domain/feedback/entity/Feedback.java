package com.slatto.domain.feedback.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.video.entity.Video;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
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

    private static final boolean DEFAULT_STATUS = false;  // 미해결

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = true)
    private Guest guest;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_time", nullable = true)
    private Long startTime;

    @Column(name = "end_time", nullable = true)
    private Long endTime;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private Feedback(Video video, Users user, Guest guest,
                     String content, Long startTime, Long endTime) {
        this.video = video;
        this.user = user;
        this.guest = guest;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = DEFAULT_STATUS;
        validateTimeRange();   // 생성 시 검증
    }

    public static Feedback create(Video video, Users user, Guest guest,
                                  String content, Long startTime, Long endTime) {
        return new Feedback(video, user, guest, content, startTime, endTime);
    }

    public void update(String content, Long startTime, Long endTime) {
        if (content != null) this.content = content;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        validateTimeRange();   // 부분 수정 후 '최종' 범위를 검증
    }

    // 타임코드 유효성 — 음수 불가, startTime <= endTime
    private void validateTimeRange() {
        if (this.startTime != null && this.startTime < 0) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        if (this.endTime != null && this.endTime < 0) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        if (this.startTime != null && this.endTime != null
                && this.startTime > this.endTime) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    public boolean isWriter(Long userId, Long guestId) {
        if (userId != null) {
            return this.user != null && this.user.getId().equals(userId);
        }
        if (guestId != null) {
            return this.guest != null && this.guest.getId().equals(guestId);
        }
        return false;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void changeStatus(Boolean status) {
        this.status = status;
    }
}