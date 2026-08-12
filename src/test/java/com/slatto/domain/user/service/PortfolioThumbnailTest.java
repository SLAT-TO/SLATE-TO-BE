package com.slatto.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
import com.slatto.domain.user.dto.PortfolioUpdateRequest;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

// 이력의 링크는 유튜브로 제한하지 않는다. 화면 안내도 "Youtube 또는 외부 링크"이므로
// 유튜브면 썸네일을 뽑고, 아니면 링크만 저장하고 썸네일을 비운다.
@DataJpaTest
@Import({PortfolioService.class, YoutubeUrlParser.class})
class PortfolioThumbnailTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VIDEO_ID = "dQw4w9WgXcQ";
    private static final String EXPECTED_THUMBNAIL =
        "https://img.youtube.com/vi/" + VIDEO_ID + "/hqdefault.jpg";

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        Users user = userRepository.save(Users.createSocialUser(
            "editor@slatto.com", "에디터", null, SocialType.GOOGLE, "social-editor"
        ));
        entityManager.flush();
        userId = user.getId();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "https://www.youtube.com/watch?v=" + VIDEO_ID,
        "https://m.youtube.com/watch?v=" + VIDEO_ID,
        "https://www.youtube.com/watch?t=10&v=" + VIDEO_ID,
        "https://youtu.be/" + VIDEO_ID,
        "https://www.youtube.com/shorts/" + VIDEO_ID,
        "https://www.youtube.com/embed/" + VIDEO_ID
    })
    @DisplayName("유튜브 링크는 형태와 무관하게 썸네일을 만든다")
    void createPortfolio_withYoutubeUrl_extractsThumbnail(String youtubeUrl) {
        PortfolioCreateResponse response = portfolioService.createPortfolio(userId, createRequest(youtubeUrl));

        assertThat(response.getThumbnailUrl()).isEqualTo(EXPECTED_THUMBNAIL);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "https://vimeo.com/123456789",
        "https://tv.naver.com/v/12345",
        "https://slatto.cloud/works/1",
        "http://www.youtube.com/watch?v=" + VIDEO_ID
    })
    @DisplayName("유튜브가 아닌 링크는 저장되고 썸네일만 비어 있다")
    void createPortfolio_withNonYoutubeUrl_savesLinkWithoutThumbnail(String externalUrl) {
        // 파서의 400 을 그대로 흘리면 사용자는 이유를 알 수 없는 저장 실패만 보게 된다.
        PortfolioCreateResponse response = portfolioService.createPortfolio(userId, createRequest(externalUrl));

        assertThat(response.getThumbnailUrl()).isNull();
        assertThat(userPortfolioRepository.findById(response.getId()).orElseThrow())
            .extracting(UserPortfolio::getYoutubeUrl)
            .isEqualTo(externalUrl);
    }

    @Test
    @DisplayName("유튜브 링크를 외부 링크로 바꾸면 남아 있던 썸네일도 지워진다")
    void updatePortfolio_toNonYoutubeUrl_clearsThumbnail() {
        // 썸네일만 예전 영상으로 남으면 카드에 엉뚱한 이미지가 붙는다.
        Long portfolioId = portfolioService
            .createPortfolio(userId, createRequest("https://youtu.be/" + VIDEO_ID))
            .getId();
        entityManager.flush();

        portfolioService.updatePortfolio(userId, portfolioId, updateRequest("https://vimeo.com/123456789"));
        entityManager.flush();
        entityManager.clear();

        assertThat(userPortfolioRepository.findById(portfolioId).orElseThrow())
            .satisfies(portfolio -> {
                assertThat(portfolio.getThumbnailUrl()).isNull();
                assertThat(portfolio.getYoutubeUrl()).isEqualTo("https://vimeo.com/123456789");
            });
    }

    private PortfolioCreateRequest createRequest(String youtubeUrl) {
        return read("""
            {"title":"연애혁명","type":"FILM_DRAMA","kind":"PERSONAL","roles":["EDITOR"],
             "description":"웹드라마 편집","youtubeUrl":"%s"}
            """.formatted(youtubeUrl), PortfolioCreateRequest.class);
    }

    private PortfolioUpdateRequest updateRequest(String youtubeUrl) {
        return read("""
            {"youtubeUrl":"%s"}
            """.formatted(youtubeUrl), PortfolioUpdateRequest.class);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
