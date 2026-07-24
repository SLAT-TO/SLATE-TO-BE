package com.slatto.domain.sharelink.service;

import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.sharelink.converter.ShareLinkConverter;
import com.slatto.domain.sharelink.dto.request.ShareLinkRequest.ShareLinkCreateReqDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.sharelink.repository.ShareLinkRepository;
import com.slatto.domain.video.entity.Video;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
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

    @Transactional
    public ShareLinkCreateResDTO createShareLink(Long videoId, ShareLinkCreateReqDTO req) {

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
                .existsByProjectIdAndUserIdAndLeftAtIsNull(projectId, req.userId());

        if (!isMember) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
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
}