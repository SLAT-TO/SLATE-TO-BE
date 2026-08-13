package com.slatto.domain.sharelink.converter;

import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkEntryResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestCreateResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestListResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.GuestSummaryResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkInfoResDTO;
import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkToggleResDTO;
import com.slatto.domain.sharelink.entity.Guest;
import com.slatto.domain.sharelink.entity.ShareLink;
import com.slatto.domain.video.entity.Video;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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

    // 해시를 받아 Guest 생성. 원문은 서비스가 보관.
    public Guest toGuest(ShareLink shareLink, String name, String sessionTokenHash) {
        return Guest.create(shareLink, name, sessionTokenHash);
    }

    // 원문 토큰은 이 응답에서만 노출. 엔티티엔 해시만 있으므로 rawSessionToken을 따로 받음.
    public GuestCreateResDTO toGuestCreateResponse(Guest guest, String rawSessionToken) {
        return new GuestCreateResDTO(
                guest.getId(),
                guest.getShareLink().getId(),
                guest.getName(),
                rawSessionToken,   // DB엔 없음, 응답 1회성
                guest.getCreatedAt()
        );
    }

    // 게스트 수는 서비스가 카운트 쿼리로 세어 넘긴다.
    public ShareLinkInfoResDTO toInfoResponse(ShareLink shareLink, long guestCount) {
        return new ShareLinkInfoResDTO(
                shareLink.getId(),
                shareLink.getVideo().getId(),
                shareLink.getToken(),
                shareLink.getIsActive(),
                shareLink.getExpiredAt(),
                shareLink.getCreatedAt(),
                guestCount
        );
    }

    public ShareLinkToggleResDTO toToggleResponse(ShareLink shareLink) {
        return new ShareLinkToggleResDTO(
                shareLink.getId(),
                shareLink.getIsActive()
        );
    }

    // 세션 토큰은 내보내지 않는다. 목록에 필요한 셋만 담는다.
    public GuestSummaryResDTO toGuestSummary(Guest guest) {
        return new GuestSummaryResDTO(
                guest.getId(),
                guest.getName(),
                guest.getCreatedAt()
        );
    }

    public GuestListResDTO toGuestListResponse(List<Guest> guests) {
        List<GuestSummaryResDTO> items = guests.stream()
                .map(this::toGuestSummary)
                .toList();

        return new GuestListResDTO(items.size(), items);
    }
}