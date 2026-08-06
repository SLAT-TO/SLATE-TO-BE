package com.slatto.domain.notification.repository;

import java.time.LocalDateTime;

public interface ProjectLatestActivityProjection {

    Long getProjectId();

    LocalDateTime getLastActivityAt();
}
