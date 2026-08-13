package com.slatto.domain.sharelink.repository;

import com.slatto.domain.sharelink.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findAllByShareLinkIdOrderByCreatedAtDesc(Long shareLinkId);

    long countByShareLinkId(Long shareLinkId);
}