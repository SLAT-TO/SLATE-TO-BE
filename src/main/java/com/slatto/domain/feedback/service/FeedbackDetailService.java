package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackDetailConverter;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
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
}