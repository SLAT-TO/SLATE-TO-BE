package com.slatto.domain.sharelink.repository;

import com.slatto.domain.sharelink.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    // 진입 검증용 — 토큰으로 조회
    Optional<ShareLink> findByToken(String token);

    // 소유자 조회용 — 영상 기준
    Optional<ShareLink> findByVideoId(Long videoId);

    // 중복 생성 방지용 (409)
    boolean existsByVideoId(Long videoId);
}