package com.slatto.domain.sharelink.converter;

import com.slatto.domain.sharelink.dto.response.ShareLinkResponse.ShareLinkCreateResDTO;
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
}