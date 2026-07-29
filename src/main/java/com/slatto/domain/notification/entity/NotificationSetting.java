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
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(name = "email_all_enabled", nullable = false)
    private Boolean emailAllEnabled = true;

    @Column(name = "email_deadline_reminder", nullable = false)
    private Boolean emailDeadlineReminder = true;

    @Column(name = "email_assigned", nullable = false)
    private Boolean emailAssigned = true;

    @Column(name = "email_new_applicant", nullable = false)
    private Boolean emailNewApplicant = true;

    @Column(name = "email_missed_summary", nullable = false)
    private Boolean emailMissedSummary = true;

    private NotificationSetting(Users user) {
        this.user = user;
        this.emailAllEnabled = true;
        this.emailDeadlineReminder = true;
        this.emailAssigned = true;
        this.emailNewApplicant = true;
        this.emailMissedSummary = true;
    }

    public static NotificationSetting createDefault(Users user) {
        return new NotificationSetting(user);
    }

    public void update(
        Boolean emailAllEnabled,
        Boolean emailDeadlineReminder,
        Boolean emailAssigned,
        Boolean emailNewApplicant,
        Boolean emailMissedSummary
    ) {
        if (emailAllEnabled != null) {
            this.emailAllEnabled = emailAllEnabled;
        }
        if (emailDeadlineReminder != null) {
            this.emailDeadlineReminder = emailDeadlineReminder;
        }
        if (emailAssigned != null) {
            this.emailAssigned = emailAssigned;
        }
        if (emailNewApplicant != null) {
            this.emailNewApplicant = emailNewApplicant;
        }
        if (emailMissedSummary != null) {
            this.emailMissedSummary = emailMissedSummary;
        }
    }
}
