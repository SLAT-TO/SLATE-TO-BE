package com.slatto.domain.sharelink.converter;

import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkEntryResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkInfoResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkToggleResDTO;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.video.entity.Video;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ShareLinkConverter {

    public ShareLink toShareLink(Video video, LocalDateTime expiredAt) {
        return ShareLink.create(video, expiredAt);
    }

    public ShareLinkCreateResDTO toCreateResponse(ShareLink shareLink) {
        return new ShareLinkCreateResDTO(
                shareLink.getId(),
                shareLink.getVideo().getId(),
                shareLink.getToken(),
                shareLink.getIsActive(),
                shareLink.getExpiredAt(),
                shareLink.getCreatedAt()
        );
    }

    public ShareLinkEntryResDTO toEntryResponse(ShareLink shareLink) {
        return new ShareLinkEntryResDTO(
                shareLink.getVideo().getId(),
                shareLink.getVideo().getTitle(),
                true   // 게스트는 항상 닉네임을 입력해야 함
        );
    }

    public Guest toGuest(ShareLink shareLink, String name) {
        return Guest.create(shareLink, name);
    }

    public GuestCreateResDTO toGuestCreateResponse(Guest guest) {
        return new GuestCreateResDTO(
                guest.getId(),
                guest.getShareLink().getId(),
                guest.getName(),
                guest.getSessionToken(),   // 발급된 세션 토큰을 응답에 포함
                guest.getCreatedAt()
        );
    }

    public ShareLinkInfoResDTO toInfoResponse(ShareLink shareLink) {
        return new ShareLinkInfoResDTO(
                shareLink.getId(),
                shareLink.getVideo().getId(),
                shareLink.getToken(),
                shareLink.getIsActive(),
                shareLink.getExpiredAt(),
                shareLink.getCreatedAt()
        );
    }

    public ShareLinkToggleResDTO toToggleResponse(ShareLink shareLink) {
        return new ShareLinkToggleResDTO(
                shareLink.getId(),
                shareLink.getIsActive()
        );
    }
}