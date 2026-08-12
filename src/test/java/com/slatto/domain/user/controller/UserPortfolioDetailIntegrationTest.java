package com.slatto.domain.user.controller;

import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPortfolioDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private EntityManager entityManager;

    private Users owner;
    private Users viewer;
    private UserPortfolio portfolio;

    @BeforeEach
    void setUp() {
        owner = saveUser("owner@example.com", "작업자", "google-owner");
        viewer = saveUser("viewer@example.com", "열람자", "google-viewer");
        portfolio = savePortfolio(owner);
    }

    @Test
    @DisplayName("다른 유저의 포트폴리오 상세를 조회한다")
    void returnsOtherUserPortfolioDetail() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/portfolios/{portfolioId}", owner.getId(), portfolio.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.id").value(portfolio.getId()))
            .andExpect(jsonPath("$.result.description").value("10분 분량 단편영화 편집"))
            .andExpect(jsonPath("$.result.comment").value("색보정까지 담당"))
            .andExpect(jsonPath("$.result.youtubeUrl").value("https://www.youtube.com/watch?v=abc123"))
            .andExpect(jsonPath("$.result.kind").value("EXTERNAL"))
            .andExpect(jsonPath("$.result.clientName").value("○○ 프로덕션"))
            .andExpect(jsonPath("$.result.roles[0]").value("EDITOR"));
    }

    @Test
    @DisplayName("포트폴리오 소유자가 아닌 userId 로 요청하면 404 다")
    void returnsNotFoundWhenUserIdDoesNotOwnPortfolio() throws Exception {
        // 남의 portfolioId 를 다른 userId 에 붙여 요청하는 경우다.
        mockMvc.perform(get("/api/v1/users/{userId}/portfolios/{portfolioId}", viewer.getId(), portfolio.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    @DisplayName("삭제된 포트폴리오는 조회되지 않는다")
    void returnsNotFoundForDeletedPortfolio() throws Exception {
        portfolio.delete();
        entityManager.flush();

        mockMvc.perform(get("/api/v1/users/{userId}/portfolios/{portfolioId}", owner.getId(), portfolio.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(viewer)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("me 경로는 유저 id 경로에 가로채이지 않는다")
    void meRouteStillResolvesToOwnPortfolio() throws Exception {
        // /users/me/portfolios/{id} 가 /users/{userId}/portfolios/{id} 로 넘어가면
        // "me" 를 Long 으로 변환하지 못해 400 이 난다.
        mockMvc.perform(get("/api/v1/users/me/portfolios/{portfolioId}", portfolio.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(portfolio.getId()));
    }

    private Users saveUser(String email, String nickname, String socialId) {
        return userRepository.save(Users.createSocialUser(
            email, nickname, null, SocialType.GOOGLE, socialId
        ));
    }

    private UserPortfolio savePortfolio(Users user) {
        UserPortfolio saved = userPortfolioRepository.save(UserPortfolio.create(
            user,
            "단편영화 편집",
            CategoryName.FILM_DRAMA,
            null,
            Kind.EXTERNAL,
            "○○ 프로덕션",
            "10분 분량 단편영화 편집",
            "색보정까지 담당",
            "https://www.youtube.com/watch?v=abc123",
            "https://img.youtube.com/vi/abc123/hqdefault.jpg",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 20)
        ));
        entityManager.persist(UserPortfolioRole.create(saved, user, RoleName.EDITOR));
        entityManager.flush();

        return saved;
    }

    private String bearerToken(Users user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId());
    }
}
