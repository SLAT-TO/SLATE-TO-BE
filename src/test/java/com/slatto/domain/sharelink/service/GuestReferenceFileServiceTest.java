package com.slatto.domain.sharelink.service;

import com.slatto.domain.project.dto.ProjectFileDownloadResponse;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.sharelink.converter.ShareLinkConverter;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.sharelink.repository.ShareLinkRepository;
import com.slatto.domain.video.dto.response.VideoResponse.GuestReferenceFileItemResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.GuestReferenceFileListResDTO;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.service.VideoReferenceFileService;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.util.TokenHasher;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuestReferenceFileServiceTest {

    private static final String SHARE_TOKEN = "share-token";
    private static final String GUEST_TOKEN = "guest-token";
    private static final Long GUEST_ID = 20L;
    private static final Long REFERENCE_FILE_ID = 7L;

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
    @DisplayName("게스트는 공유된 영상의 참고 파일 목록을 조회한다")
    void 게스트는_공유된_영상의_참고_파일_목록을_조회한다() {
        GuestReferenceFileListResDTO expected = new GuestReferenceFileListResDTO(
                List.of(new GuestReferenceFileItemResDTO(1L, "콘티.pdf", "application/pdf", 1024L, false, null)),
                null,
                false
        );
        givenValidAccess();
        given(shareLink.getVideo()).willReturn(video);
        given(videoReferenceFileService.getGuestReferenceFiles(video, "콘티", null, 20))
                .willReturn(expected);

        GuestReferenceFileListResDTO result =
                shareLinkService.getGuestReferenceFiles(SHARE_TOKEN, GUEST_ID, GUEST_TOKEN, "콘티", null, 20);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("게스트 목록 응답에는 업로더 정보가 담기지 않는다")
    void 게스트_목록_응답에는_업로더_정보가_담기지_않는다() {
        GuestReferenceFileItemResDTO item =
                new GuestReferenceFileItemResDTO(1L, "콘티.pdf", "application/pdf", 1024L, false, null);

        assertThat(item.getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("uploader", "projectFileId");
    }

    @Test
    @DisplayName("게스트는 공유된 영상의 참고 파일을 다운로드한다")
    void 게스트는_공유된_영상의_참고_파일을_다운로드한다() {
        ProjectFileDownloadResponse expected = ProjectFileDownloadResponse.builder()
                .fileName("콘티.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .build();
        givenValidAccess();
        given(shareLink.getVideo()).willReturn(video);
        given(videoReferenceFileService.downloadGuestReferenceFile(video, REFERENCE_FILE_ID))
                .willReturn(expected);

        ProjectFileDownloadResponse result = shareLinkService.downloadGuestReferenceFile(
                SHARE_TOKEN, GUEST_ID, GUEST_TOKEN, REFERENCE_FILE_ID);

        assertThat(result).isSameAs(expected);
        verify(videoReferenceFileService).downloadGuestReferenceFile(video, REFERENCE_FILE_ID);
    }

    @Test
    @DisplayName("다운로드는 공유된 영상 범위로만 위임돼, 다른 영상의 파일은 서비스가 걸러낸다")
    void 공유된_영상에_연결되지_않은_파일은_거부된다() {
        givenValidAccess();
        given(shareLink.getVideo()).willReturn(video);
        given(videoReferenceFileService.downloadGuestReferenceFile(video, 999L))
                .willThrow(new BaseException(CommonErrorCode.NOT_FOUND));

        assertThatThrownBy(() -> shareLinkService.downloadGuestReferenceFile(
                SHARE_TOKEN, GUEST_ID, GUEST_TOKEN, 999L))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("만료된 링크로는 참고 파일에 접근할 수 없다")
    void 만료된_링크로는_참고_파일에_접근할_수_없다() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(false);

        assertListError(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE, GUEST_ID, GUEST_TOKEN);
        verify(videoReferenceFileService, never()).getGuestReferenceFiles(any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 공유 링크로 등록된 게스트는 거부된다")
    void 다른_공유_링크로_등록된_게스트는_거부된다() {
        givenValidAccess();
        given(anotherShareLink.getId()).willReturn(2L);
        given(guest.getShareLink()).willReturn(anotherShareLink);

        assertListError(ShareLinkErrorCode.GUEST_ACCESS_DENIED, GUEST_ID, GUEST_TOKEN);
        verify(videoReferenceFileService, never()).getGuestReferenceFiles(any(), any(), any(), any());
    }

    @Test
    @DisplayName("게스트 토큰이 틀리면 다운로드도 거부된다")
    void 게스트_토큰이_틀리면_다운로드도_거부된다() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(true);
        given(guestRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));

        assertThatThrownBy(() -> shareLinkService.downloadGuestReferenceFile(
                SHARE_TOKEN, GUEST_ID, "wrong-token", REFERENCE_FILE_ID))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(ShareLinkErrorCode.GUEST_ACCESS_DENIED));
        verify(videoReferenceFileService, never()).downloadGuestReferenceFile(any(), anyLong());
    }

    private void givenValidAccess() {
        given(shareLinkRepository.findByToken(SHARE_TOKEN)).willReturn(Optional.of(shareLink));
        given(shareLink.isUsable()).willReturn(true);
        given(shareLink.getId()).willReturn(1L);
        given(guestRepository.findById(GUEST_ID)).willReturn(Optional.of(guest));
        given(guest.getSessionToken()).willReturn(tokenHasher.hash(GUEST_TOKEN));
        given(guest.getShareLink()).willReturn(shareLink);
    }

    private void assertListError(Object expectedCode, Long guestId, String guestToken) {
        assertThatThrownBy(() -> shareLinkService.getGuestReferenceFiles(
                SHARE_TOKEN, guestId, guestToken, null, null, null))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(expectedCode));
    }
}
