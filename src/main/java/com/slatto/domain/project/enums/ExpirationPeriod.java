package com.slatto.domain.project.enums;

import java.time.LocalDateTime;

public enum ExpirationPeriod {
    HOURS_24(24),
    HOURS_48(48),
    HOURS_72(72),
    UNLIMITED(null);

    private final Integer hours;

    ExpirationPeriod(Integer hours) {
        this.hours = hours;
    }

    public LocalDateTime calculateExpiresAt(LocalDateTime now) {
        if (this == UNLIMITED) {
            return null;
        }

        return now.plusHours(hours);
    }
}
