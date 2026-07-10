package com.slatto.domain.recruitment.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "recruitment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Users writer;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recruit_part", nullable = true, length = 255)
    private String recruitPart;

    @Column(name = "shooting_period", nullable = true, length = 255)
    private String shootingPeriod;

    @Column(name = "pay", nullable = true, length = 255)
    private String pay;

    @Column(name = "contact", nullable = true, length = 255)
    private String contact;

    @Column(name = "location", nullable = true, length = 255)
    private String location;

    @Column(name = "status", nullable = true, length = 50)
    private String status;

    @Column(name = "deadline", nullable = true)
    private LocalDate deadline;
}