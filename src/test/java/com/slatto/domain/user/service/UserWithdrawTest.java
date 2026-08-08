package com.slatto.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.auth.entity.RefreshToken;
import com.slatto.domain.auth.repository.RefreshTokenRepository;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.dto.UserWithdrawRequest;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.exception.UserErrorCode;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import com.slatto.global.exception.BaseException;
import com.slatto.global.storage.StorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserService.class, YoutubeUrlParser.class, UserWithdrawTest.PasswordEncoderTestConfig.class})
@TestPropertySource(properties = {
    "spring.jpa.database=h2",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserWithdrawTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EMAIL = "tester@slatto.com";
    private static final String PASSWORD = "slatto!2026";

    @TestConfiguration
    static class PasswordEncoderTestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    // UserService 가 프로필 이미지 업로드용으로 의존한다. 이 슬라이스에서는 빈만 제공한다.
    @MockitoBean
    private StorageService storageService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Long userId;
    private Long portfolioId;
    private Long recruitmentId;

    @BeforeEach
    void setUp() {
        Users user = userRepository.save(
            Users.createSocialUser(EMAIL, "테스터", "https://cdn.test/a.png", SocialType.GOOGLE, "social-1")
        );
        userId = user.getId();

        UserPortfolio portfolio = userPortfolioRepository.save(UserPortfolio.create(
            user,
            "연애혁명",
            CategoryName.FILM_DRAMA,
            null,
            Kind.EXTERNAL,
            "스튜디오 X",
            "웹드라마 연출",
            "감정선 중심으로 작업",
            "https://www.youtube.com/watch?v=abcdefghijk",
            "https://img.youtube.com/vi/abcdefghijk/hqdefault.jpg"
        ));
        portfolioId = portfolio.getId();

        Recruitment recruitment = recruitmentRepository.save(Recruitment.create(
            user,
            "단편영화 촬영감독 구합니다",
            CategoryName.FILM_DRAMA,
            LengthType.SHORT_FORM,
            RoleName.CINEMATOGRAPHER,
            RegionName.SEOUL,
            "2026년 9월",
            "협의",
            "010-0000-0000",
            "단편영화 촬영 인력을 모집합니다.",
            LocalDate.now().plusDays(30)
        ));
        recruitmentId = recruitment.getId();

        refreshTokenRepository.save(
            RefreshToken.issue(user, "refresh-token-value", LocalDateTime.now().plusDays(14))
        );

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("탈퇴하면 개인정보를 익명화하고 삭제 시각을 남긴다")
    void anonymizesPersonalDataOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        Users withdrawn = userRepository.findById(userId).orElseThrow();

        assertThat(withdrawn.getDeletedAt()).isNotNull();
        assertThat(withdrawn.getEmail()).isEqualTo("withdrawn_" + userId + "@slatto.invalid");
        assertThat(withdrawn.getNickname()).isEqualTo("탈퇴한 사용자_" + userId);
        assertThat(withdrawn.getSocialId()).isNull();
        assertThat(withdrawn.getProfileImageUrl()).isNull();
        assertThat(withdrawn.getBio()).isNull();
    }

    // 유저 행 자체는 남겨야 한다. 프로젝트·공고가 FK 로 참조하고 있어 지우면 이력이 끊긴다.
    @Test
    @DisplayName("탈퇴해도 유저 행은 남는다")
    void keepsUserRowOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(userId)).isPresent();
        assertThat(userRepository.findByIdAndDeletedAtIsNull(userId)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하면 포트폴리오도 함께 내려간다")
    void softDeletesPortfoliosOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        assertThat(userPortfolioRepository.findByIdAndUserIdAndDeletedAtIsNull(portfolioId, userId)).isEmpty();
        assertThat(userPortfolioRepository.findById(portfolioId).orElseThrow().getDeletedAt()).isNotNull();
    }

    // 원래 이메일이 지워져야 같은 주소로 다시 가입할 수 있다. 유니크 제약이 남으면 영구히 막힌다.
    @Test
    @DisplayName("탈퇴 후에는 같은 이메일이 조회되지 않아 재가입할 수 있다")
    void releasesEmailOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }

    // 작성자가 없는 공고가 목록에 남으면 지원자가 응답받을 수 없는 곳에 지원하게 된다.
    @Test
    @DisplayName("탈퇴하면 작성한 공고도 함께 내려간다")
    void softDeletesRecruitmentsOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        assertThat(recruitmentRepository.findById(recruitmentId).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴하면 리프레시 토큰이 삭제된다")
    void deletesRefreshTokenOnWithdraw() {
        userService.withdraw(userId, withdrawRequest());
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("refresh-token-value")).isEmpty();
    }

    // 세션만으로 탈퇴가 되면 토큰이 탈취된 상황에서 계정을 통째로 지워버릴 수 있다.
    @Test
    @DisplayName("비밀번호가 있는 계정은 비밀번호가 틀리면 탈퇴할 수 없다")
    void rejectsWithdrawWithWrongPassword() {
        Users emailUser = userRepository.save(
            Users.createEmailUser("email@slatto.com", "이메일유저", passwordEncoder.encode(PASSWORD))
        );
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> userService.withdraw(emailUser.getId(), withdrawRequest("wrong!2026")))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(UserErrorCode.WITHDRAW_PASSWORD_MISMATCH);
    }

    // 소셜로만 가입한 계정은 확인할 비밀번호가 없다. 필수로 걸면 탈퇴 자체가 막힌다.
    @Test
    @DisplayName("비밀번호가 없는 소셜 계정은 비밀번호 없이 탈퇴할 수 있다")
    void allowsWithdrawWithoutPasswordForSocialAccount() {
        userService.withdraw(userId, withdrawRequest(null));
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(userId).orElseThrow().getDeletedAt()).isNotNull();
    }

    private UserWithdrawRequest withdrawRequest() {
        return withdrawRequest(null);
    }

    private UserWithdrawRequest withdrawRequest(String password) {
        try {
            String json = password == null
                ? "{\"agreed\":true}"
                : "{\"agreed\":true,\"password\":\"" + password + "\"}";

            return OBJECT_MAPPER.readValue(json, UserWithdrawRequest.class);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

}
