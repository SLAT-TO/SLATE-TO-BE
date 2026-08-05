package com.slatto.domain.notification.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ProjectActivityReadCommandRepository {

    private static final String MYSQL_INSERT_IF_ABSENT = """
        INSERT IGNORE INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        ) VALUES (?, ?, CURRENT_TIMESTAMP)
        """;

    private static final String H2_INSERT_IF_ABSENT = """
        INSERT INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT ?, ?, CURRENT_TIMESTAMP
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
        SELECT ?, activity_log.id, CURRENT_TIMESTAMP
        FROM activity_log
        WHERE activity_log.project_id = ?
        """;

    private static final String H2_INSERT_ALL_IF_ABSENT = """
        INSERT INTO project_activity_read (
            project_member_id,
            activity_log_id,
            read_at
        )
        SELECT ?, activity_log.id, CURRENT_TIMESTAMP
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
        if (isH2()) {
            return entityManager.createNativeQuery(H2_INSERT_IF_ABSENT)
                .setParameter(1, projectMemberId)
                .setParameter(2, activityLogId)
                .setParameter(3, projectMemberId)
                .setParameter(4, activityLogId)
                .executeUpdate();
        }

        return entityManager.createNativeQuery(MYSQL_INSERT_IF_ABSENT)
            .setParameter(1, projectMemberId)
            .setParameter(2, activityLogId)
            .executeUpdate();
    }

    public int insertAllIfAbsentByProjectId(Long projectMemberId, Long projectId) {
        if (isH2()) {
            return entityManager.createNativeQuery(H2_INSERT_ALL_IF_ABSENT)
                .setParameter(1, projectMemberId)
                .setParameter(2, projectId)
                .setParameter(3, projectMemberId)
                .executeUpdate();
        }

        return entityManager.createNativeQuery(MYSQL_INSERT_ALL_IF_ABSENT)
            .setParameter(1, projectMemberId)
            .setParameter(2, projectId)
            .executeUpdate();
    }

    private boolean isH2() {
        return datasourceUrl.startsWith("jdbc:h2:");
    }
}
