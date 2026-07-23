package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackDetailConverter;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import com.slatto.domain.project.repository.ProjectMemberRepository;
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
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackDetailService {

    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final FeedbackDetailConverter feedbackDetailConverter;
    private final ProjectMemberRepository projectMemberRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Transactional
    public ReplyCreateResDTO createReply(Long feedbackId, ReplyCreateReqDTO req) {

        // 1. 원 피드백 조회 (삭제된 건 제외)
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        validateWriter(req.userId(), req.guestId());

        // 3. 작성자 조회
        Users user = null;
        Guest guest = null;

        if (req.userId() != null) {
            user = userRepository.findByIdAndDeletedAtIsNull(req.userId())
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        } else {
            guest = guestRepository.findById(req.guestId())
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        }

        // 4. 저장
        FeedbackDetail reply = feedbackDetailConverter.toFeedbackDetail(feedback, user, guest, req);
        FeedbackDetail saved = feedbackDetailRepository.save(reply);

        return feedbackDetailConverter.toCreateResponse(saved);
    }

    private void validateWriter(Long userId, Long guestId) {
        boolean hasUser = (userId != null);
        boolean hasGuest = (guestId != null);
        if (hasUser == hasGuest) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public ReplyListResDTO getReplyList(Long feedbackId, Long cursor, Integer size) {

        // 1. 원 피드백 존재 확인
        feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. size 기본값
        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        Pageable pageable = PageRequest.of(0, pageSize + 1);   // hasNext 판단용 +1

        // 3. 조회
        List<FeedbackDetail> replies = (cursor == null)
                ? feedbackDetailRepository.findFirstPage(feedbackId, pageable)
                : feedbackDetailRepository.findNextPage(feedbackId, cursor, pageable);

        // 4. hasNext 판단 + 초과분 제거
        boolean hasNext = replies.size() > pageSize;
        if (hasNext) {
            replies = replies.subList(0, pageSize);
        }

        // 5. nextCursor
        Long nextCursor = (hasNext && !replies.isEmpty())
                ? replies.getLast().getId()
                : null;

        return feedbackDetailConverter.toListResponse(replies, nextCursor, hasNext);
    }

    @Transactional
    public ReplyUpdateResDTO updateReply(Long replyId, ReplyUpdateReqDTO req) {

        // 1. 답글 조회 (삭제된 건 제외)
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        validateWriter(req.userId(), req.guestId());

        // 3. 본인 확인
        if (!reply.isWriter(req.userId(), req.guestId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 4. 수정 (더티 체킹)
        reply.update(req.content());

        return feedbackDetailConverter.toUpdateResponse(reply);
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId, Long guestId) {

        // 1. 답글 조회 (이미 삭제된 건 제외)
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        validateWriter(userId, guestId);

        // 3. 본인 확인
        if (!reply.isWriter(userId, guestId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 4. soft delete (더티 체킹)
        reply.softDelete();
    }

    @Transactional
    public ReplyStatusResDTO changeReplyStatus(Long replyId, ReplyStatusReqDTO req) {

        // 1. 답글 조회
        FeedbackDetail reply = feedbackDetailRepository.findById(replyId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 프로젝트 멤버 확인 (답글 → 피드백 → 영상 → 프로젝트)
        Long projectId = reply.getFeedback().getVideo().getProject().getId();

        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, req.userId());

        if (!isMember) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 3. 상태 변경 (더티 체킹)
        reply.changeStatus(req.status());

        return feedbackDetailConverter.toStatusResponse(reply);
    }
}