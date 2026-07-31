package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackConverter;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackUpdateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackStatusResDTO;
import com.slatto.domain.feedback.repository.FeedbackDetailRepository;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.repository.FeedbackRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
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
    private final ProjectMemberRepository projectMemberRepository;
    private final FeedbackDetailRepository feedbackDetailRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;   // 페이지 크기 상한

    @Transactional
    public FeedbackCreateResDTO createFeedback(Long videoId, Long userId, FeedbackCreateReqDTO req) {

        // 1. 영상 조회
        Video video = entityManagerProvider.getObject().createQuery("""
                        select v from Video v where v.id = :videoId
                        """, Video.class)
                .setParameter("videoId", videoId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증 — 회원(JWT userId)/게스트(body guestId) 중 정확히 하나만
        validateWriter(userId, req.guestId());

        // 3. 작성자 조회
        Users user = null;
        Guest guest = null;

        if (userId != null) {
            user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
        } else {
            // 게스트: 이 영상에 접근할 자격이 있는지 검증 후 Guest 확보
            guest = validateGuestAccess(req.guestId(), videoId);
        }

        // 4. 저장
        Feedback feedback = feedbackConverter.toFeedback(video, user, guest, req);
        Feedback saved = feedbackRepository.save(feedback);

        return feedbackConverter.toCreateResponse(saved);
    }

    @Transactional
    public FeedbackUpdateResDTO updateFeedback(Long feedbackId, Long userId, FeedbackUpdateReqDTO req) {

        // 1. 피드백 조회 (삭제된 건 제외)
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        validateWriter(userId, req.guestId());

        // 3. 게스트면 이 피드백의 영상에 접근 자격이 있는지 검증
        if (userId == null) {
            validateGuestAccess(req.guestId(), feedback.getVideo().getId());
        }

        // 4. 본인 확인
        if (!feedback.isWriter(userId, req.guestId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 5. 수정 (status 전달 안 함 — 해결 상태는 전용 API에서만 변경)
        feedback.update(req.content(), req.startTime(), req.endTime());

        // 6. updatedAt 갱신을 응답에 반영하기 위해 flush
        feedbackRepository.flush();

        return feedbackConverter.toUpdateResponse(feedback);
    }

    // 작성자 정보 검증 — userId와 guestId 중 정확히 하나만 있어야 함
    private void validateWriter(Long userId, Long guestId) {
        boolean hasUser = (userId != null);
        boolean hasGuest = (guestId != null);
        if (hasUser == hasGuest) {   // 둘 다 있거나 둘 다 없으면
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
    }

    // 게스트가 해당 영상에 접근할 자격이 있는지 검증하고, 검증된 Guest를 반환
    // Guest → ShareLink → Video 체인으로 소유 여부 확인
    private Guest validateGuestAccess(Long guestId, Long videoId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

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

    @Transactional
    public void deleteFeedback(Long feedbackId, Long userId, Long guestId) {

        // 1. 피드백 조회 (이미 삭제된 건 제외)
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 작성자 검증
        validateWriter(userId, guestId);

        // 3. 게스트면 이 피드백의 영상에 접근 자격이 있는지 검증
        if (userId == null) {
            validateGuestAccess(guestId, feedback.getVideo().getId());
        }

        // 4. 본인 확인
        if (!feedback.isWriter(userId, guestId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 5. soft delete (더티 체킹으로 자동 반영)
        feedback.softDelete();
    }

    @Transactional(readOnly = true)
    public FeedbackListResDTO getFeedbackList(Long videoId, Long userId, Long guestId, String cursor, Integer size) {

        // 1. 영상 존재 확인
        boolean videoExists = entityManagerProvider.getObject().createQuery("""
                        select count(v) from Video v where v.id = :videoId
                        """, Long.class)
                .setParameter("videoId", videoId)
                .getSingleResult() > 0;

        if (!videoExists) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }

        // 2. 게스트가 조회하는 경우 이 영상에 접근 자격이 있는지 검증
        //    회원이 아니면 guestId 필수 — 익명(둘 다 null) 조회 차단
        if (userId == null) {
            if (guestId == null) {
                throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
            }
            validateGuestAccess(guestId, videoId);
        }

        // 3. size 기본값 + 상한 처리
        int pageSize = (size == null || size <= 0)
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, pageSize + 1);   // hasNext 판단용으로 1개 더

        // 4. 커서에 따라 조회
        List<Feedback> feedbacks;

        if (cursor == null || cursor.isBlank()) {
            feedbacks = feedbackRepository.findFirstPage(videoId, pageable);
        } else {
            String[] parts = cursor.split("_");
            if (parts.length != 2) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }

            try {
                Long cursorId = Long.parseLong(parts[1]);

                if ("n".equals(parts[0])) {
                    feedbacks = feedbackRepository.findNextPageWithoutStartTime(videoId, cursorId, pageable);
                } else {
                    Long cursorStartTime = Long.parseLong(parts[0]);
                    feedbacks = feedbackRepository.findNextPageWithStartTime(videoId, cursorStartTime, cursorId, pageable);
                }
            } catch (NumberFormatException e) {
                throw new BaseException(CommonErrorCode.BAD_REQUEST);
            }
        }

        // 5. hasNext 판단 + 초과분 제거
        boolean hasNext = feedbacks.size() > pageSize;
        if (hasNext) {
            feedbacks = feedbacks.subList(0, pageSize);
        }

        // 6. nextCursor 조립
        String nextCursor = null;
        if (hasNext && !feedbacks.isEmpty()) {
            Feedback last = feedbacks.getLast();
            String timePart = (last.getStartTime() == null) ? "n" : String.valueOf(last.getStartTime());
            nextCursor = timePart + "_" + last.getId();
        }

        // 7. 답글 개수 한 번에 조회
        Map<Long, Long> replyCountMap = new HashMap<>();

        if (!feedbacks.isEmpty()) {
            List<Long> feedbackIds = feedbacks.stream()
                    .map(Feedback::getId)
                    .toList();

            for (Object[] row : feedbackDetailRepository.countByFeedbackIds(feedbackIds)) {
                replyCountMap.put((Long) row[0], (Long) row[1]);
            }
        }

        return feedbackConverter.toListResponse(feedbacks, replyCountMap, nextCursor, hasNext);
    }

    @Transactional
    public FeedbackStatusResDTO changeFeedbackStatus(Long feedbackId, Long userId, FeedbackStatusReqDTO req) {

        // 1. 피드백 조회
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .filter(f -> f.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 프로젝트 멤버인지 확인 (피드백 → 영상 → 프로젝트)
        Long projectId = feedback.getVideo().getProject().getId();

        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);

        if (!isMember) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // 3. 상태 변경 (더티 체킹)
        feedback.changeStatus(req.status());

        // 4. updatedAt 갱신 반영
        feedbackRepository.flush();

        return feedbackConverter.toStatusResponse(feedback);
    }
}