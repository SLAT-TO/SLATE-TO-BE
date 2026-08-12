package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackDetailConverter;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyStatusResDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.entity.FeedbackDetail;
import com.slatto.domain.feedback.repository.FeedbackDetailRepository;
import com.slatto.domain.feedback.repository.FeedbackRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.slatto.global.util.TokenHasher;
import com.slatto.domain.feedback.exception.FeedbackErrorCode;
import com.slatto.domain.project.exception.ProjectErrorCode;

@Service
@RequiredArgsConstructor
public class FeedbackDetailService {

    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final FeedbackDetailConverter feedbackDetailConverter;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final TokenHasher tokenHasher;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Transactional
    public ReplyCreateResDTO createReply(Long feedbackId, Long userId, Long guestId, String guestToken, ReplyCreateReqDTO req) {

        // 1. 원 피드백 조회 (삭제된 건 제외)
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        userId = resolveWriterUserId(userId, guestId);

        // 3. 작성자 조회
        Users user = null;
        Guest guest = null;

        if (userId != null) {
            user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
            validateMemberAccess(userId, feedback.getVideo().getProject().getId());
        } else {
            guest = validateGuestAccess(guestId, feedback.getVideo().getId(), guestToken);
        }

        // 4. 저장
        FeedbackDetail reply = feedbackDetailConverter.toFeedbackDetail(feedback, user, guest, req);
        FeedbackDetail saved = feedbackDetailRepository.save(reply);

        // 5. 프로젝트 멤버에게 답글 알림 발송 (작성자 본인은 actorUserId로 제외)
        String commenterName = (user != null) ? user.getNickname() : guest.getName();
        sendReplyNotification(feedback.getVideo(), userId, commenterName);

        // 6. 최근 활동 로그 기록 (회원/게스트 구분)
        if (user != null) {
            activityLogService.createVideoFeedbackCommentedLog(
                    feedback.getVideo().getProject().getId(),
                    user.getId(),
                    feedback.getVideo().getId(),
                    feedback.getVideo().getTitle()
            );
        } else {
            activityLogService.createGuestVideoFeedbackCommentedLog(
                    feedback.getVideo().getProject().getId(),
                    guest,
                    feedback.getVideo().getId(),
                    feedback.getVideo().getTitle()
            );
        }

        return feedbackDetailConverter.toCreateResponse(saved);
    }

    // 답글 생성 시 프로젝트 멤버에게 알림 발송
    private void sendReplyNotification(Video video, Long actorUserId, String commenterName) {
        Long projectId = video.getProject().getId();

        List<Long> recipientIds = projectMemberRepository
                .findAllActiveMembersByProjectId(projectId)
                .stream()
                .map(pm -> pm.getUser().getId())
                .toList();

        notificationService.createVideoFeedbackCommentedNotifications(
                projectId,
                video.getId(),
                video.getTitle(),
                commenterName,
                recipientIds,
                actorUserId
        );
    }

    // 로그인한 사람이 공유 링크로 들어오면 브라우저가 Authorization 을 자동으로 붙여
    // 회원·게스트 신원이 함께 도착한다. 게스트 헤더는 게스트 화면에서만 붙으므로
    // 그때는 게스트 의사로 보고 회원 신원을 버린다.
    private Long resolveActorUserId(Long userId, Long guestId) {
        return guestId != null ? null : userId;
    }

    // 작성자는 회원·게스트 중 하나로 확정돼야 한다.
    private Long resolveWriterUserId(Long userId, Long guestId) {
        if (userId == null && guestId == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        return resolveActorUserId(userId, guestId);
    }

    // 회원이 해당 프로젝트의 활성 멤버인지 검증
    private void validateMemberAccess(Long userId, Long projectId) {
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);
        if (!isMember) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    // 게스트가 해당 영상에 접근할 자격이 있는지 검증하고, 검증된 Guest를 반환
    private Guest validateGuestAccess(Long guestId, Long videoId, String guestToken) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 0. 세션 토큰으로 본인 확인 — 없거나 불일치면 사칭으로 간주해 차단
        if (guestToken == null || !guest.getSessionToken().equals(tokenHasher.hash(guestToken))) {
            throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
        }

        ShareLink shareLink = guest.getShareLink();

        // 1. 링크가 살아있는지 (활성 + 미만료)
        if (!shareLink.isUsable()) {
            throw new BaseException(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE);
        }

        // 2. 게스트의 링크 영상 == 요청 영상인지
        if (!shareLink.getVideo().getId().equals(videoId)) {
            throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
        }

        return guest;
    }

    @Transactional(readOnly = true)
    public ReplyListResDTO getReplyList(Long feedbackId, Long userId, Long guestId, String guestToken, Long cursor, Integer size) {

        // 1. 원 피드백 존재 확인
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        userId = resolveActorUserId(userId, guestId);

        // 2. 접근 검증 — 회원/게스트 아니면 익명 차단
        if (userId != null) {
            validateMemberAccess(userId, feedback.getVideo().getProject().getId());
        } else {
            if (guestId == null) {
                throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
            }
            validateGuestAccess(guestId, feedback.getVideo().getId(), guestToken);
        }

        // 3. size 기본값 + 상한 처리
        int pageSize = (size == null || size <= 0)
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        // 4. 조회
        List<FeedbackDetail> replies = (cursor == null)
                ? feedbackDetailRepository.findFirstPage(feedbackId, pageable)
                : feedbackDetailRepository.findNextPage(feedbackId, cursor, pageable);

        // 5. hasNext 판단 + 초과분 제거
        boolean hasNext = replies.size() > pageSize;
        if (hasNext) {
            replies = replies.subList(0, pageSize);
        }

        // 6. nextCursor
        Long nextCursor = (hasNext && !replies.isEmpty())
                ? replies.getLast().getId()
                : null;

        return feedbackDetailConverter.toListResponse(replies, nextCursor, hasNext);
    }

    @Transactional
    public ReplyUpdateResDTO updateReply(Long replyId, Long userId, Long guestId, String guestToken, ReplyUpdateReqDTO req) {

        // 1. 답글 조회 (삭제된 건 제외)
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        userId = resolveWriterUserId(userId, guestId);

        // 3. 접근 검증 (답글 → 피드백 → 영상)
        if (userId != null) {
            validateMemberAccess(userId, reply.getFeedback().getVideo().getProject().getId());
        } else {
            validateGuestAccess(guestId, reply.getFeedback().getVideo().getId(), guestToken);
        }

        // 4. 본인 확인
        if (!reply.isWriter(userId, guestId)) {
            throw new BaseException(FeedbackErrorCode.FEEDBACK_REPLY_WRITER_ONLY);
        }

        // 5. 수정
        reply.update(req.content());

        // 6. updatedAt 갱신 반영
        feedbackDetailRepository.flush();

        return feedbackDetailConverter.toUpdateResponse(reply);
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId, Long guestId, String guestToken) {

        // 1. 답글 조회 (이미 삭제된 건 제외)
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        userId = resolveWriterUserId(userId, guestId);

        // 3. 접근 검증 (답글 → 피드백 → 영상)
        if (userId != null) {
            validateMemberAccess(userId, reply.getFeedback().getVideo().getProject().getId());
        } else {
            validateGuestAccess(guestId, reply.getFeedback().getVideo().getId(), guestToken);
        }

        // 4. 본인 확인
        if (!reply.isWriter(userId, guestId)) {
            throw new BaseException(FeedbackErrorCode.FEEDBACK_REPLY_WRITER_ONLY);
        }

        // 5. soft delete
        reply.softDelete();
    }

    @Transactional
    public ReplyStatusResDTO changeReplyStatus(Long replyId, Long userId, ReplyStatusReqDTO req) {

        // 1. 답글 조회
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 프로젝트 멤버 확인 (답글 → 피드백 → 영상 → 프로젝트)
        Long projectId = reply.getFeedback().getVideo().getProject().getId();

        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);

        if (!isMember) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 3. 상태 변경
        reply.changeStatus(req.status());

        // 4. updatedAt 갱신 반영
        feedbackDetailRepository.flush();

        return feedbackDetailConverter.toStatusResponse(reply);
    }
}