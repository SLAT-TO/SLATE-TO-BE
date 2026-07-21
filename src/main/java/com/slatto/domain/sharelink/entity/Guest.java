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
}