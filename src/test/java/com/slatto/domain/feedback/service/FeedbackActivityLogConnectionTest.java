package com.slatto.domain.feedback.service;

import com.slatto.domain.feedback.converter.FeedbackConverter;
import com.slatto.domain.feedback.converter.FeedbackDetailConverter;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.entity.Feedback;
import com.slatto.domain.feedback.entity.FeedbackDetail;
import com.slatto.domain.feedback.repository.FeedbackDetailRepository;
import com.slatto.domain.feedback.repository.FeedbackRepository;
import com.slatto.domain.notification.service.ActivityLogService;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.notification.service.NotificationService;
import com.slatto.global.exception.BaseException;
import com.slatto.global.util.TokenHasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedbackActivityLogConnectionTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private FeedbackDetailRepository feedbackDetailRepository;
    @Mock private UserRepository userRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private FeedbackConverter feedbackConverter;
    @Mock private FeedbackDetailConverter feedbackDetailConverter;
    @Mock private ObjectProvider<EntityManager> entityManagerProvider;
    @Mock private EntityManager entityManager;
    @Mock private TypedQuery<Video> videoQuery;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private NotificationService notificationService;

    // 해시 비교가 실제로 돌아야 하므로 mock이 아닌 실제 인스턴스
    private final TokenHasher tokenHasher = new TokenHasher();

    private FeedbackService feedbackService;
    private FeedbackDetailService feedbackDetailService;

    private static final String GUEST_TOKEN = "test-guest-token";

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(
                feedbackRepository,
                userRepository,
                guestRepository,
                feedbackConverter,
                entityManagerProvider,
                projectMemberRepository,
                feedbackDetailRepository,
                notificationService,
                activityLogService,
                tokenHasher
        );
        feedbackDetailService = new FeedbackDetailService(
                feedbackDetailRepository,
                feedbackRepository,
                userRepository,
                guestRepository,
                feedbackDetailConverter,
                projectMemberRepository,
                notificationService,
                activityLogService,
                tokenHasher
        );
    }

    @Test
    void 회원이_피드백을_등록하면_회원_최근활동을_기록한다() {
        // 회원 피드백 저장이 끝난 뒤, 회원 ID와 영상 정보를 가진 최근활동 호출이 이어지는지 검증한다.
        Video video = video(11L, "1차 편집본", 101L);
        Users user = user(1L);
        Feedback feedback = mock(Feedback.class);

        stubVideoLookup(video);
        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
        // 정상 회원은 프로젝트 멤버 — 멤버 검증 통과하도록 stub
        given(projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(101L, 1L)).willReturn(true);
        given(feedbackConverter.toFeedback(eq(video), eq(user), eq(null), any())).willReturn(feedback);
        given(feedbackRepository.save(feedback)).willReturn(feedback);

        feedbackService.createFeedback(11L, 1L, null, null, new FeedbackCreateReqDTO("색감을 조정해주세요", 20L, 25L));

        verify(activityLogService).createVideoFeedbackCommentedLog(101L, 1L, 11L, "1차 편집본");
    }

    @Test
    void 게스트가_피드백을_등록하면_게스트_최근활동을_기록한다() {
        // 공유 링크 게스트는 회원 ID가 아닌 Guest 객체를 그대로 전달해 CLIENT_REVIEWER 활동으로 남겨야 한다.
        Video video = video(11L, "1차 편집본", 101L);
        Guest guest = mock(Guest.class);
        ShareLink shareLink = mock(ShareLink.class);
        Feedback feedback = mock(Feedback.class);

        stubVideoLookup(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        // 저장값은 원문이 아니라 해시 — 검증 시 tokenHasher.hash(원문)과 비교됨
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getVideo()).willReturn(video);
        given(feedbackConverter.toFeedback(eq(video), eq(null), eq(guest), any())).willReturn(feedback);
        given(feedbackRepository.save(feedback)).willReturn(feedback);

        feedbackService.createFeedback(11L, null, 2L, GUEST_TOKEN, new FeedbackCreateReqDTO("클라이언트 피드백입니다", 20L, 25L));

        verify(activityLogService).createGuestVideoFeedbackCommentedLog(101L, guest, 11L, "1차 편집본");
    }

    @Test
    void 회원이_답글을_등록하면_원_피드백의_영상_최근활동을_기록한다() {
        // 답글도 피드백과 같은 영상 활동으로 남아, 최근활동에서 작성 경로가 빠지지 않는지 검증한다.
        Video video = video(11L, "1차 편집본", 101L);
        Feedback feedback = mock(Feedback.class);
        Users user = user(1L);
        FeedbackDetail reply = mock(FeedbackDetail.class);

        given(feedbackRepository.findById(31L)).willReturn(Optional.of(feedback));
        given(feedback.getDeletedAt()).willReturn(null);
        given(feedback.getVideo()).willReturn(video);
        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
        // 정상 회원은 프로젝트 멤버 — 멤버 검증 통과하도록 stub
        given(projectMemberRepository.existsByProjectIdAndUserIdAndLeftAtIsNull(101L, 1L)).willReturn(true);
        given(feedbackDetailConverter.toFeedbackDetail(eq(feedback), eq(user), eq(null), any())).willReturn(reply);
        given(feedbackDetailRepository.save(reply)).willReturn(reply);

        feedbackDetailService.createReply(31L, 1L, null, null, new ReplyCreateReqDTO("반영하겠습니다"));

        verify(activityLogService).createVideoFeedbackCommentedLog(101L, 1L, 11L, "1차 편집본");
    }

    @Test
    void 게스트가_답글을_등록하면_게스트_최근활동을_기록한다() {
        // 게스트 답글도 회원 답글과 같은 영상 활동이되, 게스트 행위자 정보로 분리되어야 한다.
        Video video = video(11L, "1차 편집본", 101L);
        Feedback feedback = mock(Feedback.class);
        Guest guest = mock(Guest.class);
        ShareLink shareLink = mock(ShareLink.class);
        FeedbackDetail reply = mock(FeedbackDetail.class);

        given(feedbackRepository.findById(31L)).willReturn(Optional.of(feedback));
        given(feedback.getDeletedAt()).willReturn(null);
        given(feedback.getVideo()).willReturn(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        // 저장값은 해시
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getVideo()).willReturn(video);
        given(feedbackDetailConverter.toFeedbackDetail(eq(feedback), eq(null), eq(guest), any())).willReturn(reply);
        given(feedbackDetailRepository.save(reply)).willReturn(reply);

        feedbackDetailService.createReply(31L, null, 2L, GUEST_TOKEN, new ReplyCreateReqDTO("게스트 답글입니다"));

        verify(activityLogService).createGuestVideoFeedbackCommentedLog(101L, guest, 11L, "1차 편집본");
    }

    @Test
    void 게스트_토큰이_틀리면_피드백_등록이_차단된다() {
        // 사칭 방지: guestId는 맞아도 세션 토큰이 다르면 예외로 막히고 저장이 일어나지 않아야 한다.
        Video video = video(11L, "1차 편집본", 101L);
        Guest guest = mock(Guest.class);

        stubVideoLookup(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        // DB엔 올바른 토큰의 해시가 저장돼 있음
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));

        // 요청엔 엉뚱한 토큰 → 해시 비교 실패 → 차단
        assertThatThrownBy(() ->
                feedbackService.createFeedback(11L, null, 2L, "wrong-token",
                        new FeedbackCreateReqDTO("사칭 시도", 20L, 25L))
        ).isInstanceOf(BaseException.class);

        verify(feedbackRepository, never()).save(any());
        verify(activityLogService, never())
                .createGuestVideoFeedbackCommentedLog(any(), any(), any(), any());
    }

    @Test
    void 게스트_토큰이_없으면_피드백_등록이_차단된다() {
        // 토큰 헤더 누락(null)도 사칭 간주 → 차단.
        Video video = video(11L, "1차 편집본", 101L);
        Guest guest = mock(Guest.class);

        stubVideoLookup(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));

        assertThatThrownBy(() ->
                feedbackService.createFeedback(11L, null, 2L, null,
                        new FeedbackCreateReqDTO("토큰 없는 시도", 20L, 25L))
        ).isInstanceOf(BaseException.class);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void 게스트_토큰이_틀리면_답글_등록이_차단된다() {
        // 답글 경로도 동일하게 토큰 불일치 시 차단돼야 한다.
        Video video = video(11L, "1차 편집본", 101L);
        Feedback feedback = mock(Feedback.class);
        Guest guest = mock(Guest.class);

        given(feedbackRepository.findById(31L)).willReturn(Optional.of(feedback));
        given(feedback.getDeletedAt()).willReturn(null);
        given(feedback.getVideo()).willReturn(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));

        assertThatThrownBy(() ->
                feedbackDetailService.createReply(31L, null, 2L, "wrong-token",
                        new ReplyCreateReqDTO("사칭 답글"))
        ).isInstanceOf(BaseException.class);

        verify(feedbackDetailRepository, never()).save(any());
    }

    @Test
    void 로그인_상태로_공유_링크에서_피드백을_남기면_게스트로_기록된다() {
        // 로그인한 브라우저는 게스트 화면에서도 Authorization 을 자동으로 붙인다.
        // 두 신원이 함께 와도 게스트로 처리해야 하고, 회원 조회·멤버 검증은 타지 않아야 한다.
        Video video = video(11L, "1차 편집본", 101L);
        Guest guest = mock(Guest.class);
        ShareLink shareLink = mock(ShareLink.class);
        Feedback feedback = mock(Feedback.class);

        stubVideoLookup(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getVideo()).willReturn(video);
        given(feedbackConverter.toFeedback(eq(video), eq(null), eq(guest), any())).willReturn(feedback);
        given(feedbackRepository.save(feedback)).willReturn(feedback);

        feedbackService.createFeedback(11L, 1L, 2L, GUEST_TOKEN,
                new FeedbackCreateReqDTO("로그인한 채로 남긴 게스트 피드백", 20L, 25L));

        verify(activityLogService).createGuestVideoFeedbackCommentedLog(101L, guest, 11L, "1차 편집본");
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(projectMemberRepository, never()).existsByProjectIdAndUserIdAndLeftAtIsNull(any(), any());
    }

    @Test
    void 로그인_상태로_공유_링크에서_답글을_남기면_게스트로_기록된다() {
        // 답글 경로도 같은 규칙을 따른다.
        Video video = video(11L, "1차 편집본", 101L);
        Feedback feedback = mock(Feedback.class);
        Guest guest = mock(Guest.class);
        ShareLink shareLink = mock(ShareLink.class);
        FeedbackDetail reply = mock(FeedbackDetail.class);

        given(feedbackRepository.findById(31L)).willReturn(Optional.of(feedback));
        given(feedback.getDeletedAt()).willReturn(null);
        given(feedback.getVideo()).willReturn(video);
        given(guestRepository.findById(2L)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getVideo()).willReturn(video);
        given(feedbackDetailConverter.toFeedbackDetail(eq(feedback), eq(null), eq(guest), any())).willReturn(reply);
        given(feedbackDetailRepository.save(reply)).willReturn(reply);

        feedbackDetailService.createReply(31L, 1L, 2L, GUEST_TOKEN,
                new ReplyCreateReqDTO("로그인한 채로 남긴 게스트 답글"));

        verify(activityLogService).createGuestVideoFeedbackCommentedLog(101L, guest, 11L, "1차 편집본");
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void 회원도_게스트도_아니면_피드백_등록이_차단된다() {
        // 익명 차단은 그대로 유지된다.
        Video video = video(11L, "1차 편집본", 101L);
        stubVideoLookup(video);

        assertThatThrownBy(() ->
                feedbackService.createFeedback(11L, null, null, null,
                        new FeedbackCreateReqDTO("익명 시도", 20L, 25L))
        ).isInstanceOf(BaseException.class);

        verify(feedbackRepository, never()).save(any());
    }

    private void stubVideoLookup(Video video) {
        given(entityManagerProvider.getObject()).willReturn(entityManager);
        given(entityManager.createQuery(any(String.class), eq(Video.class))).willReturn(videoQuery);
        given(videoQuery.setParameter(eq("videoId"), eq(11L))).willReturn(videoQuery);
        given(videoQuery.getResultStream()).willReturn(Stream.of(video));
    }

    private Video video(Long videoId, String title, Long projectId) {
        Video video = mock(Video.class);
        Project project = mock(Project.class);
        given(video.getId()).willReturn(videoId);
        given(video.getTitle()).willReturn(title);
        given(video.getProject()).willReturn(project);
        given(project.getId()).willReturn(projectId);
        return video;
    }

    private Users user(Long userId) {
        Users user = mock(Users.class);
        given(user.getId()).willReturn(userId);
        return user;
    }
}