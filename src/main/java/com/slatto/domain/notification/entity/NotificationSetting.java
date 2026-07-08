package com.slatto.domain.notification.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "email_deadline_reminder", nullable = false)
    private Boolean emailDeadlineReminder = true;

    @Column(name = "email_assigned", nullable = false)
    private Boolean emailAssigned = true;

    @Column(name = "email_new_applicant", nullable = false)
    private Boolean emailNewApplicant = true;

    @Column(name = "email_missed_summary", nullable = false)
    private Boolean emailMissedSummary = true;
}