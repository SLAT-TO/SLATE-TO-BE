package com.slatto.domain.schedule.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.schedule.enums.ScheduleScope;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Users writer;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_scope", nullable = false)
    private ScheduleScope scheduleScope;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "location", nullable = true, length = 255)
    private String location;

    @Column(name = "public_memo", nullable = true, columnDefinition = "TEXT")
    private String publicMemo;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}