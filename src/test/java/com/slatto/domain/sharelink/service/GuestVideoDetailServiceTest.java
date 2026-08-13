package com.slatto.domain.sharelink.service;

import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.sharelink.converter.ShareLinkConverter;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.sharelink.repository.ShareLinkRepository;
import com.slatto.domain.video.dto.response.VideoResponse.GuestVideoDetailResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.service.VideoReferenceFileService;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.util.TokenHasher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuestVideoDetailServiceTest {

    private static final String SHARE_TOKEN = "share-token";
    private static final String GUEST_TOKEN = "guest-token";
    private static final Long GUEST_ID = 20L;

    @Mock private ShareLinkRepository shareLinkRepository;
    @Mock private ShareLinkConverter shareLinkConverter;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ObjectProvider<EntityManager> entityManagerProvider;
    @Mock private GuestRepository guestRepository;
    @Mock private VideoService videoService;
    @Mock private VideoReferenceFileService videoReferenceFileService;
    @Mock private ShareLink shareLink;
    @Mock private ShareLink anotherShareLink;
    @Mock private Guest guest;
    @Mock private Video video;

    private final TokenHasher tokenHasher = new TokenHasher();
    private ShareLinkService shareLinkService;

    @BeforeEach
    void setUp() {
        shareLinkService = new ShareLinkService(
                shareLinkRepository,
                shareLinkConverter,
                projectMemberRepository,
                entityManagerProvider,
                guestRepository,
                tokenHasher,
                videoService,
                videoReferenceFileService
        );
    }

    @Test
    void returnsGuestVideoDetailAfterValidatingLinkAndGuest() {
        GuestVideoDetailResDTO expected = new GuestVideoDetailResDTO(
                1L, "1차 편집본", "https://youtube.example/video", "video-id", "thumbnail",
                "IN_PROGRESS", "프로젝트 소개", "메모", List.of("광고"), null, null
        );
        givenValidAccess();
        given(shareLink.getVideo()).willReturn(video);
        given(videoService.getGuestVideoDetail(video)).willReturn(expected);

        GuestVideoDetailResDTO result = shareLinkService.getGuestVideo(SHARE_TOKEN, GUEST_ID, GUEST_TOKEN);

        assertThat(result).isSameAs(expected);
        verify(videoService).getGuestVideoDetail(video);
    }

    @Test
    void rejectsUnknownShareLink() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.empty());

        assertError(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND, SHARE_TOKEN, GUEST_ID, GUEST_TOKEN);
        verify(guestRepository, never()).findById(GUEST_ID);
    }

    @Test
    void rejectsUnavailableShareLink() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(false);

        assertError(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE, SHARE_TOKEN, GUEST_ID, GUEST_TOKEN);
        verify(guestRepository, never()).findById(GUEST_ID);
    }

    @Test
    void rejectsUnknownGuest() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(true);
        given(guestRepository.findById(GUEST_ID)).willReturn(Optional.empty());

        assertError(CommonErrorCode.NOT_FOUND, SHARE_TOKEN, GUEST_ID, GUEST_TOKEN);
    }

    @Test
    void rejectsInvalidGuestToken() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(true);
        given(guestRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));

        assertError(ShareLinkErrorCode.GUEST_ACCESS_DENIED, SHARE_TOKEN, GUEST_ID, "wrong-token");
        verify(videoService, never()).getGuestVideoDetail(video);
    }

    @Test
    void rejectsGuestRegisteredThroughAnotherShareLink() {
        givenValidAccess();
        given(anotherShareLink.getId()).willReturn(2L);
        given(guest.getShareLink()).willReturn(anotherShareLink);

        assertError(ShareLinkErrorCode.GUEST_ACCESS_DENIED, SHARE_TOKEN, GUEST_ID, GUEST_TOKEN);
        verify(videoService, never()).getGuestVideoDetail(video);
    }

    private void givenValidAccess() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getId()).willReturn(1L);
        given(guestRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
    }

    private void assertError(Object expectedCode, String shareToken, Long guestId, String guestToken) {
        assertThatThrownBy(() -> shareLinkService.getGuestVideo(shareToken, guestId, guestToken))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(expectedCode));
    }
}
