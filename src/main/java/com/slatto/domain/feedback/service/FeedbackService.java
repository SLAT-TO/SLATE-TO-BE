package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackConverter;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.repository.FeedbackRepository;
import com.slatto.domain.video.entity.Video;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackConverter feedbackConverter;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public FeedbackCreateResDTO createFeedback(Long videoId, FeedbackCreateReqDTO request) {
        // 1. 작성자 검증: user/guest 둘 중 하나만
        validateWriter(request.userId(), request.guestId());

        // 2. 영상 존재 확인
        Video video = entityManager.find(Video.class, videoId);
        if (video == null) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }

        // 3. 엔티티 생성(Converter) + 저장
        Feedback feedback = feedbackConverter.toFeedback(video, request);
        Feedback saved = feedbackRepository.save(feedback);

        // 4. 응답 변환(Converter)
        return feedbackConverter.toCreateResponse(saved);
    }

    private void validateWriter(Long userId, Long guestId) {
        boolean hasUser = (userId != null);
        boolean hasGuest = (guestId != null);
        if (hasUser == hasGuest) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }
}