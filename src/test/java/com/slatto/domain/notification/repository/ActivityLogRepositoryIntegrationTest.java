package com.slatto.domain.notification.repository;

import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.model.ActivityActor;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ActivityLogRepositoryIntegrationTest {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 프로젝트별_가장_최근_활동_시각만_조회한다() {
        // 홈 프로젝트 카드는 프로젝트 수정 시각이 아니라 실제 활동 로그의 최신 발생 시각을 보여줘야 한다.
        Users owner = userRepository.save(Users.createSocialUser(
            "chataehun@example.com", "차태훈", null, SocialType.GOOGLE, "google-chataehun"
        ));
        Project firstProject = saveProject(owner, "첫 번째 프로젝트");
        Project secondProject = saveProject(owner, "두 번째 프로젝트");
        LocalDateTime firstProjectLatest = LocalDateTime.of(2026, 8, 5, 10, 30);
        LocalDateTime secondProjectLatest = LocalDateTime.of(2026, 8, 5, 11, 45);

        saveActivity(firstProject, owner, LocalDateTime.of(2026, 8, 5, 9, 0));
        saveActivity(firstProject, owner, firstProjectLatest);
        saveActivity(secondProject, owner, secondProjectLatest);
        entityManager.flush();
        entityManager.clear();

        Map<Long, LocalDateTime> lastActivityAtByProjectId = activityLogRepository
            .findLatestActivityAtByProjectIds(List.of(firstProject.getId(), secondProject.getId()))
            .stream()
            .collect(Collectors.toMap(
                ProjectLatestActivityProjection::getProjectId,
                ProjectLatestActivityProjection::getLastActivityAt
            ));

        assertThat(lastActivityAtByProjectId)
            .containsEntry(firstProject.getId(), firstProjectLatest)
            .containsEntry(secondProject.getId(), secondProjectLatest);
    }

    @Test
    void 활동이_없는_프로젝트는_최신_활동_집계에서_제외한다() {
        // 활동 이력이 없는 카드는 lastActivityAt=null로 내려가므로 FE가 경과 시간 문구를 숨길 수 있어야 한다.
        Users owner = userRepository.save(Users.createSocialUser(
            "green@example.com", "그린", null, SocialType.GOOGLE, "google-green"
        ));
        Project projectWithoutActivity = saveProject(owner, "활동 없는 프로젝트");

        List<ProjectLatestActivityProjection> projections = activityLogRepository
            .findLatestActivityAtByProjectIds(List.of(projectWithoutActivity.getId()));

        assertThat(projections).isEmpty();
    }

    private Project saveProject(Users owner, String title) {
        return projectRepository.save(Project.create(
            owner,
            title,
            CategoryName.DOCUMENTARY,
            LengthType.SHORT_FORM,
            "프로젝트 목록 최신 활동 시각 검증용 설명입니다.",
            LocalDate.now().plusDays(1),
            null,
            Kind.PERSONAL
        ));
    }

    private void saveActivity(Project project, Users actor, LocalDateTime createdAt) {
        ActivityLog activityLog = activityLogRepository.save(ActivityLog.create(
            project,
            ActivityActor.user(actor.getId(), actor.getNickname()),
            ActivityLogType.FILE_UPLOADED,
            "차태훈님이 파일을 등록했어요.",
            ActivityLogTargetType.FILE,
            1L
        ));
        ReflectionTestUtils.setField(activityLog, "createdAt", createdAt);
    }
}
