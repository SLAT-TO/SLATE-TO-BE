package com.slatto.domain.sharelink.service;

import com.slatto.domain.project.dto.ProjectFileDownloadResponse;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.sharelink.converter.ShareLinkConverter;
import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.ShareLinkCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkEntryResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkInfoResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkToggleResDTO;
import com.slatto.domain.sharelink.repository.GuestRepository;
import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.GuestCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestCreateResDTO;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.sharelink.repository.ShareLinkRepository;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.dto.response.VideoResponse.GuestReferenceFileListResDTO;
import com.slatto.domain.video.dto.response.VideoResponse.GuestVideoDetailResDTO;
import com.slatto.domain.video.service.VideoReferenceFileService;
import com.slatto.domain.video.service.VideoService;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.util.TokenHasher;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkConverter shareLinkConverter;
    private final ProjectMemberRepository projectMemberRepository;
    private final ObjectProvider<EntityManager> entityManagerProvider;
    private final GuestRepository guestRepository;
    private final TokenHasher tokenHasher;
    private final VideoService videoService;
    private final VideoReferenceFileService videoReferenceFileService;

    @Transactional
    public ShareLinkCreateResDTO createShareLink(Long videoId, Long userId, ShareLinkCreateReqDTO req) {

        // 1. 영상 조회
        Video video = entityManagerProvider.getObject().createQuery("""
                        select v from Video v where v.id = :videoId
                        """, Video.class)
                .setParameter("videoId", videoId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 프로젝트 멤버인지 확인 (영상 → 프로젝트)
        Long projectId = video.getProject().getId();
        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);

        if (!isMember) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 3. 만료 일시가 과거면 거부
        if (req.expiredAt() != null && !req.expiredAt().isAfter(LocalDateTime.now())) {
            throw new BaseException(ShareLinkErrorCode.INVALID_EXPIRED_AT);
        }

        // 4. 영상당 링크는 1개만 (중복 시 409)
        if (shareLinkRepository.existsByVideoId(videoId)) {
            throw new BaseException(ShareLinkErrorCode.SHARE_LINK_ALREADY_EXISTS);
        }

        // 5. 생성 (토큰은 엔티티가 UUID로 발급)
        ShareLink shareLink = shareLinkConverter.toShareLink(video, req.expiredAt());
        ShareLink saved = shareLinkRepository.save(shareLink);

        return shareLinkConverter.toCreateResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShareLinkEntryResDTO getShareLinkByToken(String token) {

        // 1. 토큰으로 조회
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BaseException(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND));

        // 2. 비활성화되었거나 만료되었으면 410
        if (!shareLink.isUsable()) {
            throw new BaseException(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE);
        }

        return shareLinkConverter.toEntryResponse(shareLink);
    }

    @Transactional
    public GuestCreateResDTO registerGuest(String token, GuestCreateReqDTO req) {

        // 1. 토큰으로 링크 조회
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BaseException(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND));

        // 2. 사용 가능한 링크인지 (비활성/만료면 410)
        if (!shareLink.isUsable()) {
            throw new BaseException(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE);
        }

        // 3. 세션 토큰 발급 — 원문은 응답에만, DB에는 해시 저장
        String rawSessionToken = UUID.randomUUID().toString();
        String sessionTokenHash = tokenHasher.hash(rawSessionToken);

        // 4. 게스트 생성 (해시 저장)
        Guest guest = shareLinkConverter.toGuest(shareLink, req.name(), sessionTokenHash);
        Guest saved = guestRepository.save(guest);

        // 5. 응답에 원문 토큰 포함 (이후 요청 시 X-Guest-Token 헤더로 재전송)
        return shareLinkConverter.toGuestCreateResponse(saved, rawSessionToken);
    }

    @Transactional(readOnly = true)
    public GuestVideoDetailResDTO getGuestVideo(String shareToken, Long guestId, String guestToken) {
        ShareLink shareLink = getShareLinkForGuest(shareToken, guestId, guestToken);

        return videoService.getGuestVideoDetail(shareLink.getVideo());
    }

    @Transactional(readOnly = true)
    public GuestReferenceFileListResDTO getGuestReferenceFiles(
            String shareToken,
            Long guestId,
            String guestToken,
            String keyword,
            Long cursor,
            Integer size
    ) {
        ShareLink shareLink = getShareLinkForGuest(shareToken, guestId, guestToken);

        return videoReferenceFileService.getGuestReferenceFiles(shareLink.getVideo(), keyword, cursor, size);
    }

    @Transactional(readOnly = true)
    public ProjectFileDownloadResponse downloadGuestReferenceFile(
            String shareToken,
            Long guestId,
            String guestToken,
            Long referenceFileId
    ) {
        ShareLink shareLink = getShareLinkForGuest(shareToken, guestId, guestToken);

        return videoReferenceFileService.downloadGuestReferenceFile(shareLink.getVideo(), referenceFileId);
    }

    /** 살아 있는 링크인지, 그 링크로 등록된 게스트가 맞는지 확인하고 링크를 돌려준다. */
    private ShareLink getShareLinkForGuest(String shareToken, Long guestId, String guestToken) {
        ShareLink shareLink = shareLinkRepository.findByToken(shareToken)
                .orElseThrow(() -> new BaseException(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND));

        if (!shareLink.isUsable()) {
            throw new BaseException(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE);
        }

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        if (guestToken == null || !guest.getSessionToken().equals(tokenHasher.hash(guestToken))) {
            throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
        }

        if (!guest.getShareLink().getId().equals(shareLink.getId())) {
            throw new BaseException(ShareLinkErrorCode.GUEST_ACCESS_DENIED);
        }

        return shareLink;
    }

    @Transactional(readOnly = true)
    public ShareLinkInfoResDTO getShareLinkByVideo(Long videoId, Long userId) {

        // 1. 영상 조회
        Video video = entityManagerProvider.getObject().createQuery("""
                        select v from Video v where v.id = :videoId
                        """, Video.class)
                .setParameter("videoId", videoId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        // 2. 프로젝트 멤버인지 확인
        Long projectId = video.getProject().getId();

        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);

        if (!isMember) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 3. 링크 조회 (없으면 404)
        ShareLink shareLink = shareLinkRepository.findByVideoId(videoId)
                .orElseThrow(() -> new BaseException(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND));

        return shareLinkConverter.toInfoResponse(shareLink);
    }

    @Transactional
    public ShareLinkToggleResDTO toggleShareLink(Long shareLinkId, Long userId) {

        // 1. 링크 조회
        ShareLink shareLink = shareLinkRepository.findById(shareLinkId)
                .orElseThrow(() -> new BaseException(ShareLinkErrorCode.SHARE_LINK_NOT_FOUND));

        // 2. 프로젝트 멤버인지 확인 (링크 → 영상 → 프로젝트)
        Long projectId = shareLink.getVideo().getProject().getId();

        boolean isMember = projectMemberRepository
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, userId);

        if (!isMember) {
            throw new BaseException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 3. 상태 토글 (더티 체킹)
        shareLink.toggleActive();

        return shareLinkConverter.toToggleResponse(shareLink);
    }
}
