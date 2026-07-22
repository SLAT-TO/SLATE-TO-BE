package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.NotificationSettingResponse;
import com.slatto.domain.notification.dto.NotificationSettingUpdateRequest;
import com.slatto.domain.notification.entity.NotificationSetting;
import com.slatto.domain.notification.repository.NotificationSettingRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

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
            .orElseGet(() -> {
                notificationSettingRepository.insertDefaultIfAbsent(userId);
                return notificationSettingRepository.findByUserId(userId)
                    .orElseThrow(() -> new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR));
            });
    }
}
