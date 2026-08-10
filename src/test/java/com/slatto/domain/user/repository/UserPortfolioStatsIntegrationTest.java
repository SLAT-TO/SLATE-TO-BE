package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserPortfolioStatsIntegrationTest {

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Users user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(Users.createSocialUser(
            "user@example.com", "사용자", null, SocialType.GOOGLE, "google-user"
        ));
    }

    @Test
    @DisplayName("프로젝트 유형을 건수 내림차순으로 집계한다")
    void aggregatesProjectTypesByCountDesc() {
        savePortfolio(CategoryName.FILM_DRAMA);
        savePortfolio(CategoryName.FILM_DRAMA);
        savePortfolio(CategoryName.MUSIC_VIDEO);

        List<Object[]> rows = userPortfolioRepository.findProjectTypeStatRowsByUserId(user.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)[0]).isEqualTo(CategoryName.FILM_DRAMA);
        assertThat(rows.get(0)[1]).isEqualTo(2L);
        assertThat(rows.get(1)[0]).isEqualTo(CategoryName.MUSIC_VIDEO);
        assertThat(rows.get(1)[1]).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제한 포트폴리오는 유형 집계에서 빠진다")
    void excludesDeletedPortfolioFromTypeStats() {
        savePortfolio(CategoryName.FILM_DRAMA);
        UserPortfolio deleted = savePortfolio(CategoryName.MUSIC_VIDEO);
        deleted.delete();
        userPortfolioRepository.saveAndFlush(deleted);

        List<Object[]> rows = userPortfolioRepository.findProjectTypeStatRowsByUserId(user.getId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0]).isEqualTo(CategoryName.FILM_DRAMA);
    }

    @Test
    @DisplayName("역할을 건수 내림차순으로 집계한다")
    void aggregatesRolesByCountDesc() {
        UserPortfolio first = savePortfolio(CategoryName.FILM_DRAMA);
        UserPortfolio second = savePortfolio(CategoryName.MUSIC_VIDEO);
        saveRole(first, RoleName.DIRECTOR);
        saveRole(first, RoleName.EDITOR);
        saveRole(second, RoleName.DIRECTOR);

        List<Object[]> rows = userPortfolioRepository.findRoleStatRowsByUserId(user.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)[0]).isEqualTo(RoleName.DIRECTOR);
        assertThat(rows.get(0)[1]).isEqualTo(2L);
        assertThat(rows.get(1)[0]).isEqualTo(RoleName.EDITOR);
        assertThat(rows.get(1)[1]).isEqualTo(1L);
    }

    @Test
    @DisplayName("포트폴리오를 삭제하면 그 역할도 집계에서 빠진다")
    void excludesRolesOfDeletedPortfolio() {
        // user_portfolio_role 에는 deleted_at 이 없다. 부모의 soft delete 를 조인으로 걸러야 한다.
        UserPortfolio kept = savePortfolio(CategoryName.FILM_DRAMA);
        UserPortfolio deleted = savePortfolio(CategoryName.MUSIC_VIDEO);
        saveRole(kept, RoleName.DIRECTOR);
        saveRole(deleted, RoleName.EDITOR);
        deleted.delete();
        userPortfolioRepository.saveAndFlush(deleted);

        List<Object[]> rows = userPortfolioRepository.findRoleStatRowsByUserId(user.getId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0]).isEqualTo(RoleName.DIRECTOR);
    }

    @Test
    @DisplayName("포트폴리오가 없으면 빈 결과를 돌려준다")
    void returnsEmptyWhenNoPortfolio() {
        assertThat(userPortfolioRepository.findProjectTypeStatRowsByUserId(user.getId())).isEmpty();
        assertThat(userPortfolioRepository.findRoleStatRowsByUserId(user.getId())).isEmpty();
    }

    private UserPortfolio savePortfolio(CategoryName type) {
        return userPortfolioRepository.save(UserPortfolio.create(
            user, "작업 제목", type, null, Kind.PERSONAL, null, null, null, null, null
        ));
    }

    private void saveRole(UserPortfolio portfolio, RoleName roleName) {
        entityManager.persist(UserPortfolioRole.create(portfolio, user, roleName));
        entityManager.flush();
    }
}
