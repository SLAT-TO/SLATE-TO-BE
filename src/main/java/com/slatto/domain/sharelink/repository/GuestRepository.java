package com.slatto.domain.sharelink.repository;

import com.slatto.domain.sharelink.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}