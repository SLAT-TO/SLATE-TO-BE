package com.slatto.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.user.dto.PortfolioUpdateRequest;
import com.slatto.domain.user.dto.PortfolioUpdateResponse;
import com.slatto.domain.user.dto.UserProfileUpdateRequest;
import com.slatto.domain.user.dto.UserProfileUpdateResponse;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserPortfolioRoleRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PortfolioService.class, UserService.class, YoutubeUrlParser.class})
@TestPropertySource(properties = {
    "spring.jpa.database=h2",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PortfolioUpdatedAtTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private UserPortfolioRoleRepository userPortfolioRoleRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;
    private Long portfolioId;
    private LocalDateTime initialUpdatedAt;

    @BeforeEach
    void setUp() throws InterruptedException {
        Users user = userRepository.save(
            Users.createSocialUser("tester@slatto.com", "tester", null, SocialType.GOOGLE, "social-1")
        );

        UserPortfolio portfolio = userPortfolioRepository.save(UserPortfolio.create(
            user,
            "연애혁명",
            CategoryName.FILM_DRAMA,
            null,
            Kind.EXTERNAL,
            "스튜디오 X",
            "웹드라마 연출 및 편집",
            "감정선 중심으로 작업",
            "https://www.youtube.com/watch?v=abcdefghijk",
            "https://img.youtube.com/vi/abcdefghijk/hqdefault.jpg"
        ));
        userPortfolioRoleRepository.save(UserPortfolioRole.create(portfolio, user, RoleName.DIRECTOR));

        entityManager.flush();

        userId = user.getId();
        portfolioId = portfolio.getId();
        initialUpdatedAt = portfolio.getUpdatedAt();

        Thread.sleep(20);
    }

    @Test
    @DisplayName("roles만 수정해도 updatedAt이 갱신된다")
    void updatePortfolio_rolesOnly_refreshesUpdatedAt() {
        PortfolioUpdateResponse response = portfolioService.updatePortfolio(
            userId,
            portfolioId,
            request("{\"roles\":[\"EDITOR\",\"PD\"]}", PortfolioUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(findPortfolio().getUpdatedAt());
        assertThat(currentRoleNames()).containsExactly(RoleName.EDITOR, RoleName.PD);
    }

    @Test
    @DisplayName("본체 필드만 수정하면 기존대로 updatedAt이 갱신된다")
    void updatePortfolio_bodyOnly_refreshesUpdatedAt() {
        PortfolioUpdateResponse response = portfolioService.updatePortfolio(
            userId,
            portfolioId,
            request("{\"title\":\"연애혁명 (감독판)\"}", PortfolioUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(findPortfolio().getUpdatedAt());
        assertThat(findPortfolio().getTitle()).isEqualTo("연애혁명 (감독판)");
        assertThat(currentRoleNames()).containsExactly(RoleName.DIRECTOR);
    }

    @Test
    @DisplayName("본체 필드와 roles를 함께 수정해도 updatedAt이 하나의 값으로 갱신된다")
    void updatePortfolio_bodyAndRoles_refreshesUpdatedAtOnce() {
        PortfolioUpdateResponse response = portfolioService.updatePortfolio(
            userId,
            portfolioId,
            request("{\"title\":\"연애혁명 (감독판)\",\"roles\":[\"EDITOR\"]}", PortfolioUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isAfter(initialUpdatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(findPortfolio().getUpdatedAt());
        assertThat(findPortfolio().getTitle()).isEqualTo("연애혁명 (감독판)");
        assertThat(currentRoleNames()).containsExactly(RoleName.EDITOR);
    }

    @Test
    @DisplayName("빈 요청은 아무것도 수정하지 않으므로 updatedAt이 유지된다")
    void updatePortfolio_emptyRequest_keepsUpdatedAt() {
        PortfolioUpdateResponse response = portfolioService.updatePortfolio(
            userId,
            portfolioId,
            request("{}", PortfolioUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isEqualTo(initialUpdatedAt);
        assertThat(currentRoleNames()).containsExactly(RoleName.DIRECTOR);
    }

    @Test
    @DisplayName("프로필 수정에서 roles만 수정해도 updatedAt이 갱신된다")
    void updateProfile_rolesOnly_refreshesUpdatedAt() {
        LocalDateTime beforeUpdatedAt = findUser().getUpdatedAt();

        UserProfileUpdateResponse response = userService.updateProfile(
            userId,
            request("{\"roles\":[\"EDITOR\"]}", UserProfileUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isAfter(beforeUpdatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(findUser().getUpdatedAt());
        assertThat(response.getRoles()).containsExactly(RoleName.EDITOR);
    }

    @Test
    @DisplayName("프로필 수정에서 본체 필드만 수정하면 기존대로 updatedAt이 갱신된다")
    void updateProfile_bodyOnly_refreshesUpdatedAt() {
        LocalDateTime beforeUpdatedAt = findUser().getUpdatedAt();

        UserProfileUpdateResponse response = userService.updateProfile(
            userId,
            request("{\"bio\":\"영상 편집자입니다.\"}", UserProfileUpdateRequest.class)
        );

        assertThat(response.getUpdatedAt()).isAfter(beforeUpdatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(findUser().getUpdatedAt());
    }

    private <T> T request(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UserPortfolio findPortfolio() {
        return userPortfolioRepository.findById(portfolioId).orElseThrow();
    }

    private Users findUser() {
        return userRepository.findById(userId).orElseThrow();
    }

    private List<RoleName> currentRoleNames() {
        return userPortfolioRoleRepository.findAllByPortfolioIdOrderByIdAsc(portfolioId)
            .stream()
            .map(UserPortfolioRole::getRoleName)
            .toList();
    }
}
