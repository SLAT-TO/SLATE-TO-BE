package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackConverter;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
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
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));   // ✅ 404

        // 2. 작성자 조회 — 회원이면 user, 게스트면 guest (둘 중 하나만)
        Users user = null;
        Guest guest = null;

        if (req.userId() != null) {
            user = userRepository.findByIdAndDeletedAtIsNull(req.userId())
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));   // ✅ 404
        } else if (req.guestId() != null) {
            guest = guestRepository.findById(req.guestId())
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));   // ✅ 404
        } else {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);                       // ✅ 400
        }

        // 3. Converter로 엔티티 만들고 저장
        Feedback feedback = feedbackConverter.toFeedback(video, user, guest, req);
        Feedback saved = feedbackRepository.save(feedback);

        // 4. 저장된 엔티티 → 응답 DTO
        return feedbackConverter.toCreateResponse(saved);
    }
}