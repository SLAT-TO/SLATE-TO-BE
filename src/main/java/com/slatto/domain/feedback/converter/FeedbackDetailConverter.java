package com.slatto.domain.feedback.converter;

import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.ActorDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListItemDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import java.util.List;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.entity.FeedbackDetail;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.user.entity.Users;
import org.springframework.stereotype.Component;

@Component
public class FeedbackDetailConverter {

    public FeedbackDetail toFeedbackDetail(Feedback feedback, Users user, Guest guest,
                                           ReplyCreateReqDTO req) {
        return FeedbackDetail.create(feedback, user, guest, req.content());
    }

    public ReplyCreateResDTO toCreateResponse(FeedbackDetail reply) {
        ActorDTO actor = (reply.getUser() != null)
                ? ActorDTO.fromUser(reply.getUser())
                : ActorDTO.fromGuest(reply.getGuest());

        return new ReplyCreateResDTO(
                reply.getId(),
                reply.getFeedback().getId(),
                actor,
                reply.getContent(),
                reply.getStatus(),
                reply.getCreatedAt()
        );
    }

    public ReplyListItemDTO toListItem(FeedbackDetail reply) {
        ActorDTO actor = (reply.getUser() != null)
                ? ActorDTO.fromUser(reply.getUser())
                : ActorDTO.fromGuest(reply.getGuest());

        return new ReplyListItemDTO(
                reply.getId(),
                reply.getFeedback().getId(),
                actor,
                reply.getContent(),
                reply.getStatus(),
                reply.getCreatedAt()
        );
    }

    public ReplyListResDTO toListResponse(List<FeedbackDetail> replies, Long nextCursor, boolean hasNext) {
        List<ReplyListItemDTO> items = replies.stream()
                .map(this::toListItem)
                .toList();

        return new ReplyListResDTO(items, nextCursor, hasNext);
    }

    public ReplyUpdateResDTO toUpdateResponse(FeedbackDetail reply) {
        ActorDTO actor = (reply.getUser() != null)
                ? ActorDTO.fromUser(reply.getUser())
                : ActorDTO.fromGuest(reply.getGuest());

        return new ReplyUpdateResDTO(
                reply.getId(),
                reply.getFeedback().getId(),
                actor,
                reply.getContent(),
                reply.getStatus(),
                reply.getUpdatedAt()
        );
    }
}