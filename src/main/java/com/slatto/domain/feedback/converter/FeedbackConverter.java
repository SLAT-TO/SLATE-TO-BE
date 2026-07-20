package com.slatto.domain.feedback.converter;

import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.video.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class FeedbackConverter {

    public Feedback toFeedback(Video video, FeedbackCreateReqDTO request) {
        return Feedback.create(
                video,
                request.userId(),
                request.guestId(),
                request.content()
        );
    }

    public FeedbackCreateResDTO toCreateResponse(Feedback feedback) {
        return new FeedbackCreateResDTO(
                feedback.getId(),
                feedback.getVideo().getId(),
                feedback.getUserId(),
                feedback.getGuestId(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getCreatedAt()
        );
    }
}