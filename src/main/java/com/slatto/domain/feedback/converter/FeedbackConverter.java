package com.slatto.domain.feedback.converter;

import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackUpdateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.ActorDTO;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.sharelink.entity.Guest;
import org.springframework.stereotype.Component;

@Component
public class FeedbackConverter {

    public Feedback toFeedback(Video video, Users user, Guest guest,
                               FeedbackCreateReqDTO req) {
        return Feedback.create(
                video, user, guest,
                req.content(), req.startTime(), req.endTime()
        );
    }

    public FeedbackCreateResDTO toCreateResponse(Feedback feedback) {
        ActorDTO actor = (feedback.getUser() != null)
                ? ActorDTO.fromUser(feedback.getUser())
                : ActorDTO.fromGuest(feedback.getGuest());

        return new FeedbackCreateResDTO(
                feedback.getId(),
                feedback.getVideo().getId(),
                actor,
                feedback.getContent(),
                feedback.getStartTime(),
                feedback.getEndTime(),
                feedback.getStatus(),
                feedback.getCreatedAt()
        );
    }

    public FeedbackUpdateResDTO toUpdateResponse(Feedback feedback) {
        ActorDTO actor = (feedback.getUser() != null)
                ? ActorDTO.fromUser(feedback.getUser())
                : ActorDTO.fromGuest(feedback.getGuest());

        return new FeedbackUpdateResDTO(
                feedback.getId(),
                feedback.getVideo().getId(),
                actor,
                feedback.getContent(),
                feedback.getStartTime(),
                feedback.getEndTime(),
                feedback.getStatus(),
                feedback.getUpdatedAt()
        );
    }
}