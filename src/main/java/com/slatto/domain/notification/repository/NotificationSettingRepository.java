package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserId(Long userId);

    // user_id unique 제약을 이용해 동시 요청에서도 최대 한 행만 생성되도록 원자적으로 처리한다.
    // 충돌 시 UPDATE는 no-op이라 예외 없이 멱등하게 동작한다.
    @Modifying
    @Query(value = """
        INSERT INTO notification_setting
            (user_id, email_all_enabled, email_deadline_reminder, email_assigned,
             email_new_applicant, email_missed_summary, created_at, updated_at)
        VALUES (:userId, true, true, true, true, true, NOW(6), NOW(6))
        ON DUPLICATE KEY UPDATE user_id = user_id
        """, nativeQuery = true)
    void insertDefaultIfAbsent(@Param("userId") Long userId);
}
