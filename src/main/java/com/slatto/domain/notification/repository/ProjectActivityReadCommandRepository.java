package com.slatto.domain.notification.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;

@Repository
public class ProjectActivityReadCommandRepository {

    // 시각은 DB 의 CURRENT_TIMESTAMP 가 아니라 애플리케이션에서 만든다.
    // DB 시계는 MySQL 세션 타임존을 따르므로 JPA Auditing 이 쓰는 다른 시각들과 어긋난다.
    private static final String MYSQL_INSERT_IF_ABSENT = """
        INSERT IGNORE INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        ) VALUES (?, ?, ?)
        """;

    private static final String H2_INSERT_IF_ABSENT = """
        INSERT INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT ?, ?, ?
        WHERE NOT EXISTS (
            SELECT 1
            FROM project_activity_read
            WHERE project_member_id = ?
              AND activity_log_id = ?
        )
        """;

    private static final String MYSQL_INSERT_ALL_IF_ABSENT = """
        INSERT IGNORE INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT ?, activity_log.id, ?
        FROM activity_log
        WHERE activity_log.project_id = ?
        """;

    private static final String H2_INSERT_ALL_IF_ABSENT = """
        INSERT INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT ?, activity_log.id, ?
        FROM activity_log
        WHERE activity_log.project_id = ?
          AND NOT EXISTS (
              SELECT 1
              FROM project_activity_read
              WHERE project_activity_read.project_member_id = ?
                AND project_activity_read.activity_log_id = activity_log.id
          )
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public int insertIfAbsent(Long projectMemberId, Long activityLogId) {
        LocalDateTime now = LocalDateTime.now();

        if (isH2()) {
            return entityManager.createNativeQuery(H2_INSERT_IF_ABSENT)
                .setParameter(1, projectMemberId)
                .setParameter(2, activityLogId)
                .setParameter(3, now)
                .setParameter(4, projectMemberId)
                .setParameter(5, activityLogId)
                .executeUpdate();
        }

        return entityManager.createNativeQuery(MYSQL_INSERT_IF_ABSENT)
            .setParameter(1, projectMemberId)
            .setParameter(2, activityLogId)
            .setParameter(3, now)
            .executeUpdate();
    }

    public int insertAllIfAbsentByProjectId(Long projectMemberId, Long projectId) {
        LocalDateTime now = LocalDateTime.now();

        if (isH2()) {
            return entityManager.createNativeQuery(H2_INSERT_ALL_IF_ABSENT)
                .setParameter(1, projectMemberId)
                .setParameter(2, now)
                .setParameter(3, projectId)
                .setParameter(4, projectMemberId)
                .executeUpdate();
        }

        return entityManager.createNativeQuery(MYSQL_INSERT_ALL_IF_ABSENT)
            .setParameter(1, projectMemberId)
            .setParameter(2, now)
            .setParameter(3, projectId)
            .executeUpdate();
    }

    private boolean isH2() {
        return datasourceUrl.startsWith("jdbc:h2:");
    }
}
