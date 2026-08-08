package com.slatto.domain.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.exception.RecruitmentErrorCode;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.enums.SocialType;
import com.slatto.global.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentCloseEditTest {

    private static final Long WRITER_ID = 1L;
    private static final Long RECRUITMENT_ID = 10L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private RecruitmentRepository recruitmentRepository;

    // 마감 판정 로직을 그대로 써야 검증이 의미가 있어 실제 구현을 넣는다.
    @Spy
    private RecruitmentConverter recruitmentConverter = new RecruitmentConverter();

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Test
    @DisplayName("마감된 공고의 내용 수정은 거부한다")
    void rejectsContentEditOnClosedRecruitment() {
        givenRecruitment(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> recruitmentService.updateRecruitment(
            WRITER_ID, RECRUITMENT_ID, request("{\"title\":\"새 제목으로 변경\"}")
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(RecruitmentErrorCode.RECRUITMENT_CLOSED_NOT_EDITABLE);
    }

    // status 만 RECRUITING 으로 보내도 마감일이 과거면 여전히 마감이다.
    // 이걸 통과시키면 같은 요청에 실린 내용 변경까지 반영돼 수정 금지가 우회된다.
    @Test
    @DisplayName("마감일이 과거인데 status 만 RECRUITING 으로 보내면 거부한다")
    void rejectsReopenWithoutFutureDeadline() {
        givenRecruitment(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> recruitmentService.updateRecruitment(
            WRITER_ID, RECRUITMENT_ID,
            request("{\"status\":\"RECRUITING\",\"title\":\"우회로 바꾼 제목\"}")
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(RecruitmentErrorCode.RECRUITMENT_CLOSED_NOT_EDITABLE);
    }

    @Test
    @DisplayName("과거 마감일을 그대로 다시 보내도 거부한다")
    void rejectsReopenWithPastDeadline() {
        givenRecruitment(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> recruitmentService.updateRecruitment(
            WRITER_ID, RECRUITMENT_ID,
            request("{\"status\":\"RECRUITING\",\"deadline\":\""
                + LocalDate.now().minusDays(3) + "\"}")
        ))
            .isInstanceOf(BaseException.class)
            .extracting(exception -> ((BaseException) exception).getErrorCode())
            .isEqualTo(RecruitmentErrorCode.RECRUITMENT_CLOSED_NOT_EDITABLE);
    }

    private void givenRecruitment(LocalDate deadline) {
        Users writer = Users.createSocialUser("writer@slatto.com", "작성자", null, SocialType.GOOGLE, "social-1");
        ReflectionTestUtils.setField(writer, "id", WRITER_ID);

        Recruitment recruitment = Recruitment.create(
            writer,
            "단편영화 촬영감독 구합니다",
            CategoryName.FILM_DRAMA,
            LengthType.SHORT_FORM,
            RoleName.CINEMATOGRAPHER,
            RegionName.SEOUL,
            "2026년 9월",
            "협의",
            "010-0000-0000",
            "촬영 인력을 모집합니다.",
            deadline
        );
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);

        when(recruitmentRepository.findActiveWithWriterById(RECRUITMENT_ID))
            .thenReturn(Optional.of(recruitment));
    }

    private RecruitmentUpdateRequest request(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, RecruitmentUpdateRequest.class);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

}
