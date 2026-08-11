package com.slatto.domain.sharelink.entity;

import com.slatto.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "guest")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id", nullable = false)
    private ShareLink shareLink;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // 게스트 본인 확인용 세션 토큰. 등록 시 발급되며, 이후 요청 시 이 값과 일치해야 본인으로 인정한다.
    @Column(name = "session_token", nullable = false, unique = true, length = 36)
    private String sessionToken;

    private Guest(ShareLink shareLink, String name, String sessionToken) {
        this.shareLink = shareLink;
        this.name = name;
        this.sessionToken = sessionToken;
    }

    public static Guest create(ShareLink shareLink, String name) {
        return new Guest(shareLink, name, UUID.randomUUID().toString());
    }
}