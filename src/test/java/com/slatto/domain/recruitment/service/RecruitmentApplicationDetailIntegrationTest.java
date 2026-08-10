package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.dto.RecruitmentApplicationDetailResponse;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RecruitmentApplicationDetailIntegrationTest {

    @Autowired
    private RecruitmentApplicationService recruitmentApplicationService;

    @Autowired
    private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Users writer;
    private Users applicant;
    private Recruitment recruitment;
    private RecruitmentApplication application;

    @BeforeEach
    void setUp() {
        writer = saveUser("writer@example.com", "작성자", "google-writer");
        applicant = saveUser("applicant@example.com", "지원자", "google-applicant");
        applicant.updateProfile("지원자", "영상 편집 5년차입니다", null);

        recruitment = recruitmentRepository.save(Recruitment.create(
            writer, "공고 제목입니다", CategoryName.FILM_DRAMA, null, RoleName.DIRECTOR,
            RegionName.SEOUL, null, null, "010-0000-0000", "설명", null
        ));
        application = recruitmentApplicationRepository.save(
            RecruitmentApplication.create(applicant, recruitment, "지원합니다", "https://portfolio.example")
        );
    }

    @Test
    @DisplayName("지원 상세가 지원자 프로필과 프로젝트 이력을 함께 반환한다")
    void returnsApplicantProfileAndPortfolios() {
        savePortfolio(CategoryName.FILM_DRAMA, RoleName.EDITOR);

        RecruitmentApplicationDetailResponse response = recruitmentApplicationService.getApplicationDetail(
            writer.getId(), recruitment.getId(), application.getId()
        );

        RecruitmentApplicationDetailResponse.ApplicantProfile profile = response.getApplicant();
        assertThat(profile.getBio()).isEqualTo("영상 편집 5년차입니다");
        assertThat(profile.getPortfolios().getItems()).hasSize(1);
        assertThat(profile.getStats().getProjectTypes()).hasSize(1);
        assertThat(profile.getStats().getRoles()).hasSize(1);
    }

    @Test
    @DisplayName("포트폴리오가 5건을 넘으면 hasNext 로 더 있음을 알린다")
    void marksHasNextWhenPortfolioExceedsLimit() {
        for (int i = 0; i < 6; i++) {
            savePortfolio(CategoryName.FILM_DRAMA, RoleName.EDITOR);
        }

        RecruitmentApplicationDetailResponse response = recruitmentApplicationService.getApplicationDetail(
            writer.getId(), recruitment.getId(), application.getId()
        );

        assertThat(response.getApplicant().getPortfolios().getItems()).hasSize(5);
        assertThat(response.getApplicant().getPortfolios().getHasNext()).isTrue();
        assertThat(response.getApplicant().getPortfolios().getNextCursor()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴한 지원자의 지원도 상세 조회된다")
    void returnsDetailForWithdrawnApplicant() {
        // 지원자 목록은 탈퇴 여부를 거르지 않는다. 상세만 404 가 되면 작성자가 목록에서 연 항목이 열리지 않는다.
        applicant.withdraw(LocalDateTime.now());
        entityManager.flush();

        RecruitmentApplicationDetailResponse response = recruitmentApplicationService.getApplicationDetail(
            writer.getId(), recruitment.getId(), application.getId()
        );

        assertThat(response.getApplicant().getId()).isEqualTo(applicant.getId());
    }

    @Test
    @DisplayName("공고 작성자도 지원 본인도 아니면 상세를 볼 수 없다")
    void deniesDetailToStranger() {
        Users stranger = saveUser("stranger@example.com", "제삼자", "google-stranger");

        assertThatThrownBy(() -> recruitmentApplicationService.getApplicationDetail(
            stranger.getId(), recruitment.getId(), application.getId()
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    private Users saveUser(String email, String nickname, String socialId) {
        return userRepository.save(Users.createSocialUser(
            email, nickname, null, SocialType.GOOGLE, socialId
        ));
    }

    private void savePortfolio(CategoryName type, RoleName roleName) {
        UserPortfolio portfolio = userPortfolioRepository.save(UserPortfolio.create(
            applicant, "작업 제목", type, null, Kind.PERSONAL, null, null, null, null, null
        ));
        entityManager.persist(UserPortfolioRole.create(portfolio, applicant, roleName));
        entityManager.flush();
    }
}
