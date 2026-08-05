package com.slatto.domain.notification.service;

import com.slatto.domain.notification.dto.ActivityLogListResponse;
import com.slatto.domain.notification.entity.ActivityLog;
import com.slatto.domain.notification.entity.ProjectActivityRead;
import com.slatto.domain.notification.enums.ActivityLogTargetType;
import com.slatto.domain.notification.enums.ActivityLogType;
import com.slatto.domain.notification.model.ActivityActor;
import com.slatto.domain.notification.repository.ActivityLogRepository;
import com.slatto.domain.notification.repository.ProjectActivityReadRepository;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.project.repository.ProjectMemberRepository;
import com.slatto.domain.project.repository.ProjectRepository;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({RecentActivityService.class, ProjectAccessValidator.class})
class RecentActivityServiceIntegrationTest {

    @Autowired
    private RecentActivityService recentActivityService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ProjectActivityReadRepository projectActivityReadRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 같은_시각의_활동도_ID_기준_cursor로_중복없이_다음_페이지를_조회한다() {
        // 같은 초에 여러 활동이 저장돼도 createdAt과 ID 복합 커서로 이전 페이지 항목을 다시 주면 안 된다.
        Fixture fixture = createFixture();
        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 8, 4, 9, 30);
        ActivityLog first = saveActivity(fixture.project(), fixture.user(), "첫 번째 활동", sameCreatedAt);
        ActivityLog second = saveActivity(fixture.project(), fixture.user(), "두 번째 활동", sameCreatedAt);
        ActivityLog third = saveActivity(fixture.project(), fixture.user(), "세 번째 활동", sameCreatedAt);
        entityManager.flush();
        entityManager.clear();

        ActivityLogListResponse firstPage = recentActivityService.getRecentActivities(
            fixture.project().getId(), fixture.user().getId(), null, 2
        );
        ActivityLogListResponse secondPage = recentActivityService.getRecentActivities(
            fixture.project().getId(), fixture.user().getId(), firstPage.nextCursor(), 2
        );

        assertThat(firstPage.items()).extracting(ActivityLogListResponse.ActivityLogItem::activityId)
            .containsExactly(third.getId(), second.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.items()).extracting(ActivityLogListResponse.ActivityLogItem::activityId)
            .containsExactly(first.getId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 개별_확인_후에는_선택한_활동만_읽음으로_반환한다() {
        // 최근활동 하나를 눌러도 클릭하지 않은 이전·이후 활동의 새 활동 배지는 유지해야 한다.
        Fixture fixture = createFixture();
        LocalDateTime base = LocalDateTime.of(2026, 8, 4, 10, 0);
        ActivityLog oldest = saveActivity(fixture.project(), fixture.user(), "가장 이전 활동", base);
        ActivityLog selected = saveActivity(fixture.project(), fixture.user(), "선택한 활동", base.plusMinutes(1));
        ActivityLog newest = saveActivity(fixture.project(), fixture.user(), "가장 최근 활동", base.plusMinutes(2));
        entityManager.flush();

        recentActivityService.markActivityAsRead(
            fixture.project().getId(), selected.getId(), fixture.user().getId()
        );
        entityManager.flush();
        entityManager.clear();

        List<ActivityLogListResponse.ActivityLogItem> items = recentActivityService.getRecentActivities(
            fixture.project().getId(), fixture.user().getId(), null, 10
        ).items();

        assertThat(findItem(items, newest.getId()).isNew()).isTrue();
        assertThat(findItem(items, selected.getId()).isNew()).isFalse();
        assertThat(findItem(items, oldest.getId()).isNew()).isTrue();
    }

    @Test
    void 한_멤버의_개별_확인은_다른_멤버의_새_활동_상태에_영향을_주지_않는다() {
        // 프로젝트의 다른 멤버도 같은 활동을 보지만, 읽음 여부는 멤버별로 독립적이어야 한다.
        Fixture fixture = createFixture();
        Users green = userRepository.save(Users.createSocialUser(
            "green@example.com",
            "그린",
            null,
            SocialType.GOOGLE,
            "google-green"
        ));
        projectMemberRepository.save(ProjectMember.createMember(fixture.project(), green));
        ActivityLog activity = saveActivity(
            fixture.project(),
            fixture.user(),
            "차태훈님이 새 공지를 등록했어요.",
            LocalDateTime.of(2026, 8, 4, 10, 0)
        );
        entityManager.flush();

        recentActivityService.markActivityAsRead(
            fixture.project().getId(),
            activity.getId(),
            fixture.user().getId()
        );
        entityManager.flush();
        entityManager.clear();

        List<ActivityLogListResponse.ActivityLogItem> chaTaehoonItems = recentActivityService.getRecentActivities(
            fixture.project().getId(), fixture.user().getId(), null, 10
        ).items();
        List<ActivityLogListResponse.ActivityLogItem> greenItems = recentActivityService.getRecentActivities(
            fixture.project().getId(), green.getId(), null, 10
        ).items();

        assertThat(findItem(chaTaehoonItems, activity.getId()).isNew()).isFalse();
        assertThat(findItem(greenItems, activity.getId()).isNew()).isTrue();
    }

    @Test
    void 다른_프로젝트의_활동은_개별_확인할_수_없다() {
        // 활동 ID를 바꿔 호출해도 현재 프로젝트 외부의 읽음 시각을 갱신하지 못해야 한다.
        Fixture fixture = createFixture();
        Project otherProject = saveProject(fixture.user(), "다른 프로젝트");
        projectMemberRepository.save(ProjectMember.createMember(otherProject, fixture.user()));
        ActivityLog otherProjectActivity = saveActivity(
            otherProject,
            fixture.user(),
            "다른 프로젝트 활동",
            LocalDateTime.of(2026, 8, 4, 11, 0)
        );

        assertThatThrownBy(() -> recentActivityService.markActivityAsRead(
            fixture.project().getId(), otherProjectActivity.getId(), fixture.user().getId()
        )).isInstanceOf(BaseException.class);
    }

    @Test
    void 프로젝트_비참여자는_최근활동을_조회할_수_없다() {
        // 프로젝트에 속하지 않은 사용자는 활동 내용과 새 활동 여부 모두 확인할 수 없어야 한다.
        Fixture fixture = createFixture();
        Users nonMember = userRepository.save(Users.createSocialUser(
            "green@example.com",
            "그린",
            null,
            SocialType.GOOGLE,
            "google-green"
        ));

        assertThatThrownBy(() -> recentActivityService.getRecentActivities(
            fixture.project().getId(), nonMember.getId(), null, 20
        )).isInstanceOf(BaseException.class);
    }

    @Test
    void 전체_확인은_현재_멤버의_읽지_않은_활동만_읽음으로_저장한다() {
        // 전체 읽음은 현재 멤버에게만 적용하고, 이미 읽은 활동의 읽음 행을 중복 생성하지 않아야 한다.
        Fixture fixture = createFixture();
        ActivityLog first = saveActivity(
            fixture.project(), fixture.user(), "첫 번째 활동", LocalDateTime.of(2026, 8, 4, 10, 0)
        );
        ActivityLog second = saveActivity(
            fixture.project(), fixture.user(), "두 번째 활동", LocalDateTime.of(2026, 8, 4, 10, 1)
        );
        entityManager.flush();

        recentActivityService.markAllActivitiesAsRead(fixture.project().getId(), fixture.user().getId());
        recentActivityService.markAllActivitiesAsRead(fixture.project().getId(), fixture.user().getId());
        entityManager.flush();
        entityManager.clear();

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserIdAndLeftAtIsNull(
            fixture.project().getId(), fixture.user().getId()
        ).orElseThrow();
        List<ProjectActivityRead> reads = projectActivityReadRepository.findAll();

        assertThat(reads)
            .extracting(read -> read.getProjectMember().getId(), read -> read.getActivityLog().getId())
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(member.getId(), first.getId()),
                org.assertj.core.groups.Tuple.tuple(member.getId(), second.getId())
            );
    }

    private Fixture createFixture() {
        Users user = userRepository.save(Users.createSocialUser(
            "chataehun@example.com",
            "차태훈",
            null,
            SocialType.GOOGLE,
            "google-chataehun"
        ));
        Project project = saveProject(user, "최근활동 조회 테스트 프로젝트");
        projectMemberRepository.save(ProjectMember.createAdmin(project, user));

        return new Fixture(user, project);
    }

    private Project saveProject(Users owner, String title) {
        return projectRepository.save(Project.create(
            owner,
            title,
            CategoryName.DOCUMENTARY,
            LengthType.SHORT_FORM,
            "최근활동 조회와 읽음 처리 검증용 프로젝트입니다.",
            LocalDate.now().plusDays(1),
            null,
            Kind.PERSONAL
        ));
    }

    private ActivityLog saveActivity(Project project, Users actor, String content, LocalDateTime createdAt) {
        ActivityLog activityLog = activityLogRepository.save(ActivityLog.create(
            project,
            ActivityActor.user(actor.getId(), actor.getNickname()),
            ActivityLogType.FILE_UPLOADED,
            content,
            ActivityLogTargetType.FILE,
            100L
        ));
        ReflectionTestUtils.setField(activityLog, "createdAt", createdAt);
        return activityLog;
    }

    private ActivityLogListResponse.ActivityLogItem findItem(
        List<ActivityLogListResponse.ActivityLogItem> items,
        Long activityId
    ) {
        return items.stream()
            .filter(item -> item.activityId().equals(activityId))
            .findFirst()
            .orElseThrow();
    }

    private record Fixture(Users user, Project project) {
    }
}
