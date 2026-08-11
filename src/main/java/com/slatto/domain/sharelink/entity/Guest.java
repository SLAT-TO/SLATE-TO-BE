package com.slatto.domain.sharelink.entity;

import com.slatto.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 세션 토큰의 SHA-256 해시(64자 hex). 원문은 저장하지 않는다.
    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String sessionToken;

    private Guest(ShareLink shareLink, String name, String sessionTokenHash) {
        this.shareLink = shareLink;
        this.name = name;
        this.sessionToken = sessionTokenHash;
    }

    // 원문이 아니라 '해시'를 받아 저장한다. 원문 생성은 서비스가 담당.
    public static Guest create(ShareLink shareLink, String name, String sessionTokenHash) {
        return new Guest(shareLink, name, sessionTokenHash);
    }
}