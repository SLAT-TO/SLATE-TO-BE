package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackConverter;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackUpdateResDTO;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.repository.FeedbackRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.entity.Video;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final FeedbackConverter feedbackConverter;
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Transactional
    public FeedbackCreateResDTO createFeedback(Long videoId, FeedbackCreateReqDTO req) {

        // 1. 영상 조회 — videoId만으로 (EntityManager 직접, 임시)
        Video video = entityManagerProvider.getObject().createQuery("""
                        select v from Video v where v.id = :videoId
                        """, Video.class)
                .setParameter("videoId", videoId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증 — userId/guestId 둘 중 정확히 하나만
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

        // 4. Converter로 엔티티 만들고 저장
        Feedback feedback = feedbackConverter.toFeedback(video, user, guest, req);
        Feedback saved = feedbackRepository.save(feedback);

        // 5. 저장된 엔티티 → 응답 DTO
        return feedbackConverter.toCreateResponse(saved);
    }

    @Transactional
    public FeedbackUpdateResDTO updateFeedback(Long feedbackId, FeedbackUpdateReqDTO req) {

        // 1. 피드백 조회 (삭제된 건 제외)
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증 — userId/guestId 둘 중 정확히 하나만
        validateWriter(req.userId(), req.guestId());

        // 3. 본인 확인
        if (!feedback.isWriter(req.userId(), req.guestId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 4. 수정 (더티 체킹으로 자동 반영)
        feedback.update(req.content(), req.startTime(), req.endTime(), req.status());

        return feedbackConverter.toUpdateResponse(feedback);
    }

    /**
     * 작성자 정보 검증 — userId와 guestId 중 정확히 하나만 있어야 함
     */
    private void validateWriter(Long userId, Long guestId) {
        boolean hasUser = (userId != null);
        boolean hasGuest = (guestId != null);
        if (hasUser == hasGuest) {   // 둘 다 있거나 둘 다 없으면
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }
}