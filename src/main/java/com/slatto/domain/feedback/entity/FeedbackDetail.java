package com.slatto.domain.feedback.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.user.entity.Users;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
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

    private static final boolean DEFAULT_STATUS = false;  // 미해결

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = true)
    private Guest guest;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private FeedbackDetail(Feedback feedback, Users user, Guest guest, String content) {
        // 작성자는 회원/게스트 중 정확히 하나여야 함
        if ((user == null) == (guest == null)) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        this.feedback = feedback;
        this.user = user;
        this.guest = guest;
        this.content = content;
        this.status = DEFAULT_STATUS;
    }

    public static FeedbackDetail create(Feedback feedback, Users user, Guest guest, String content) {
        return new FeedbackDetail(feedback, user, guest, content);
    }

    public void update(String content) {
        if (content != null) this.content = content;
    }

    public void changeStatus(Boolean status) {
        this.status = status;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
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
}