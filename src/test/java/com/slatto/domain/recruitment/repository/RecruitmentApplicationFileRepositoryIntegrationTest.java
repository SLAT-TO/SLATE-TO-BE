package com.slatto.domain.recruitment.repository;

import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.entity.RecruitmentApplicationFile;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecruitmentApplicationFileRepositoryIntegrationTest {

    @Autowired
    private RecruitmentApplicationFileRepository recruitmentApplicationFileRepository;

    @Autowired
    private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Users writer;
    private Users applicant;
    private Users stranger;
    private Recruitment recruitment;

    @BeforeEach
    void setUp() {
        writer = saveUser("writer@example.com", "작성자", "google-writer");
        applicant = saveUser("applicant@example.com", "지원자", "google-applicant");
        stranger = saveUser("stranger@example.com", "제삼자", "google-stranger");
        recruitment = saveRecruitment();
    }

    @Test
    @DisplayName("본인이 올린 미연결 파일만 연결 대상으로 조회된다")
    void findsOnlyOwnUnlinkedFiles() {
        RecruitmentApplicationFile mine = saveFile(recruitment, applicant);
        RecruitmentApplicationFile others = saveFile(recruitment, stranger);

        List<RecruitmentApplicationFile> linkable = recruitmentApplicationFileRepository.findLinkableFiles(
            List.of(mine.getId(), others.getId()), recruitment.getId(), applicant.getId()
        );

        assertThat(linkable).extracting(RecruitmentApplicationFile::getId).containsExactly(mine.getId());
    }

    @Test
    @DisplayName("이미 다른 지원에 연결된 파일은 재사용할 수 없다")
    void excludesAlreadyLinkedFiles() {
        RecruitmentApplicationFile file = saveFile(recruitment, applicant);
        RecruitmentApplication application = recruitmentApplicationRepository.save(
            RecruitmentApplication.create(applicant, recruitment, "지원합니다", null)
        );
        file.linkTo(application);
        recruitmentApplicationFileRepository.saveAndFlush(file);

        List<RecruitmentApplicationFile> linkable = recruitmentApplicationFileRepository.findLinkableFiles(
            List.of(file.getId()), recruitment.getId(), applicant.getId()
        );

        assertThat(linkable).isEmpty();
    }

    @Test
    @DisplayName("다른 공고에 올린 파일은 연결 대상이 아니다")
    void excludesFilesFromOtherRecruitment() {
        Recruitment otherRecruitment = saveRecruitment();
        RecruitmentApplicationFile file = saveFile(otherRecruitment, applicant);

        List<RecruitmentApplicationFile> linkable = recruitmentApplicationFileRepository.findLinkableFiles(
            List.of(file.getId()), recruitment.getId(), applicant.getId()
        );

        assertThat(linkable).isEmpty();
    }

    @Test
    @DisplayName("삭제된 파일은 연결 대상에서 빠진다")
    void excludesDeletedFiles() {
        RecruitmentApplicationFile file = saveFile(recruitment, applicant);
        file.softDelete();
        recruitmentApplicationFileRepository.saveAndFlush(file);

        List<RecruitmentApplicationFile> linkable = recruitmentApplicationFileRepository.findLinkableFiles(
            List.of(file.getId()), recruitment.getId(), applicant.getId()
        );

        assertThat(linkable).isEmpty();
    }

    @Test
    @DisplayName("지원에 연결된 첨부 파일을 조회한다")
    void findsFilesByApplicationId() {
        RecruitmentApplication application = recruitmentApplicationRepository.save(
            RecruitmentApplication.create(applicant, recruitment, "지원합니다", null)
        );
        RecruitmentApplicationFile linked = saveFile(recruitment, applicant);
        linked.linkTo(application);
        recruitmentApplicationFileRepository.saveAndFlush(linked);
        saveFile(recruitment, applicant);

        List<RecruitmentApplicationFile> files =
            recruitmentApplicationFileRepository.findByApplicationIds(List.of(application.getId()));

        assertThat(files).extracting(RecruitmentApplicationFile::getId).containsExactly(linked.getId());
    }

    private Users saveUser(String email, String nickname, String socialId) {
        return userRepository.save(Users.createSocialUser(
            email, nickname, null, SocialType.GOOGLE, socialId
        ));
    }

    private Recruitment saveRecruitment() {
        return recruitmentRepository.save(Recruitment.create(
            writer, "공고 제목입니다", CategoryName.FILM_DRAMA, null, RoleName.DIRECTOR,
            RegionName.SEOUL, null, null, "010-0000-0000", "설명", null
        ));
    }

    private RecruitmentApplicationFile saveFile(Recruitment target, Users uploader) {
        return recruitmentApplicationFileRepository.save(RecruitmentApplicationFile.create(
            target, uploader, "포트폴리오.pdf", "application/pdf", 1024L,
            "recruitments/%d/applications/%s.pdf".formatted(target.getId(), uploader.getId())
        ));
    }
}
