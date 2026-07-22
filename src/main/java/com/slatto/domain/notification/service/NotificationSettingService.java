package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.NotificationSettingResponse;
import com.slatto.domain.notification.dto.NotificationSettingUpdateRequest;
import com.slatto.domain.notification.entity.NotificationSetting;
import com.slatto.domain.notification.repository.NotificationSettingRepository;
import com.slatto.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    public NotificationSettingResponse getMySettings(Long userId) {
        return NotificationSettingResponse.from(getOrCreate(userId));
    }

    public NotificationSettingResponse updateMySettings(Long userId, NotificationSettingUpdateRequest request) {
        NotificationSetting setting = getOrCreate(userId);
        setting.update(
            request.getEmailAllEnabled(),
            request.getEmailDeadlineReminder(),
            request.getEmailAssigned(),
            request.getEmailNewApplicant(),
            request.getEmailMissedSummary()
        );

        return NotificationSettingResponse.from(setting);
    }

    private NotificationSetting getOrCreate(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
            .orElseGet(() -> notificationSettingRepository.save(
                NotificationSetting.createDefault(userRepository.getReferenceById(userId))
            ));
    }
}
