package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecruitmentMultiFilterIntegrationTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 9);

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Users writer;

    @BeforeEach
    void setUp() {
        writer = userRepository.save(Users.createSocialUser(
            "writer@example.com", "작성자", null, SocialType.GOOGLE, "google-writer"
        ));

        save("서울 연출 영화", CategoryName.FILM_DRAMA, RoleName.DIRECTOR, RegionName.SEOUL);
        save("경기 촬영 광고", CategoryName.AD_BRAND, RoleName.CINEMATOGRAPHER, RegionName.GYEONGGI);
        save("경남 연출 다큐", CategoryName.DOCUMENTARY, RoleName.DIRECTOR, RegionName.GYEONGSANGNAM);
    }

    @Test
    @DisplayName("지역을 복수로 넘기면 OR 로 묶여 조회된다")
    void filtersByMultipleLocations() {
        List<Recruitment> results = findLatest(null, null, List.of(RegionName.SEOUL, RegionName.GYEONGGI));

        assertThat(results)
            .extracting(Recruitment::getLocation)
            .containsExactlyInAnyOrder(RegionName.SEOUL, RegionName.GYEONGGI);
    }

    @Test
    @DisplayName("서로 다른 필터는 AND 로 결합된다")
    void combinesDifferentFiltersWithAnd() {
        List<Recruitment> results = findLatest(
            null,
            List.of(RoleName.DIRECTOR),
            List.of(RegionName.SEOUL, RegionName.GYEONGGI)
        );

        assertThat(results)
            .extracting(Recruitment::getTitle)
            .containsExactly("서울 연출 영화");
    }

    @Test
    @DisplayName("필터를 null 로 넘기면 in 절이 무시되고 전체가 조회된다")
    void returnsAllWhenFiltersAreNull() {
        assertThat(findLatest(null, null, null)).hasSize(3);
        assertThat(findDeadline(null, null, null)).hasSize(3);
        assertThat(findPopular(null, null, null)).hasSize(3);
    }

    @Test
    @DisplayName("세 필터를 동시에 복수로 넘겨도 동작한다")
    void filtersByAllThreeCollectionsAtOnce() {
        List<Recruitment> results = findLatest(
            List.of(CategoryName.FILM_DRAMA, CategoryName.DOCUMENTARY),
            List.of(RoleName.DIRECTOR),
            List.of(RegionName.SEOUL, RegionName.GYEONGSANGNAM)
        );

        assertThat(results)
            .extracting(Recruitment::getTitle)
            .containsExactlyInAnyOrder("서울 연출 영화", "경남 연출 다큐");
    }

    @Test
    @DisplayName("마감순·인기순 정렬에서도 복수 필터가 적용된다")
    void appliesMultiFilterOnEverySortType() {
        List<CategoryName> categories = List.of(CategoryName.FILM_DRAMA, CategoryName.AD_BRAND);

        assertThat(findDeadline(categories, null, null)).hasSize(2);
        assertThat(findPopular(categories, null, null)).hasSize(2);
    }

    private List<Recruitment> findLatest(
        List<CategoryName> categories,
        List<RoleName> recruitParts,
        List<RegionName> locations
    ) {
        return recruitmentRepository.findPageOrderByLatest(
            null, categories, null, recruitParts, locations, null, TODAY, null, FIRST_PAGE
        );
    }

    private List<Recruitment> findDeadline(
        List<CategoryName> categories,
        List<RoleName> recruitParts,
        List<RegionName> locations
    ) {
        return recruitmentRepository.findPageOrderByDeadline(
            null, categories, null, recruitParts, locations, null, TODAY, null, null, null, FIRST_PAGE
        );
    }

    private List<Recruitment> findPopular(
        List<CategoryName> categories,
        List<RoleName> recruitParts,
        List<RegionName> locations
    ) {
        return recruitmentRepository.findPageOrderByPopular(
            null, categories, null, recruitParts, locations, null, TODAY, null, null, FIRST_PAGE
        );
    }

    private void save(String title, CategoryName category, RoleName recruitPart, RegionName location) {
        recruitmentRepository.save(Recruitment.create(
            writer, title, category, null, recruitPart, location, null, null, "010-0000-0000", "설명", null
        ));
    }
}
