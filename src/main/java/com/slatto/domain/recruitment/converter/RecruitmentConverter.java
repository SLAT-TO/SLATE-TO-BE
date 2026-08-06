package com.slatto.domain.recruitment.converter;

import com.slatto.domain.recruitment.dto.MyApplicationListResponse;
import com.slatto.domain.recruitment.dto.MyRecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicantListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentBookmarkResponse;
import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentRecommendationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentSummary;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RecruitmentConverter {

    // 이 도메인에서 현재 날짜를 얻는 유일한 지점이다. 타임존을 고칠 때 여기 한 줄만 고친다.
    // 서비스는 요청당 한 번만 호출해 쿼리 바인딩과 표시 계산에 같은 값을 쓴다.
    public LocalDate currentDate() {
        return LocalDate.now();
    }

    public Recruitment toRecruitment(Users writer, RecruitmentCreateRequest request) {
        return Recruitment.create(
            writer,
            request.getTitle(),
            request.getCategory(),
            request.getLengthType(),
            request.getRecruitPart(),
            request.getLocation(),
            request.getShootingPeriod(),
            request.getPay(),
            request.getContact(),
            request.getDescription(),
            request.getDeadline()
        );
    }

    public RecruitmentDetailResponse toDetailResponse(
        Recruitment recruitment,
        Long currentUserId,
        long applicantCount,
        boolean isBookmarked,
        RecruitmentApplicationStatus myApplicationStatus,
        RoleName writerPrimaryRole,
        List<RegionName> writerRegions
    ) {
        LocalDate today = currentDate();

        return RecruitmentDetailResponse.builder()
            .id(recruitment.getId())
            .title(recruitment.getTitle())
            .category(recruitment.getCategory())
            .lengthType(recruitment.getLengthType())
            .recruitPart(recruitment.getRecruitPart())
            .location(recruitment.getLocation())
            .pay(recruitment.getPay())
            .deadline(recruitment.getDeadline())
            .dday(calculateDday(recruitment.getDeadline(), today))
            .status(resolveStatus(recruitment.getClosedManually(), recruitment.getDeadline(), today))
            .description(recruitment.getDescription())
            .shootingPeriod(recruitment.getShootingPeriod())
            .contact(recruitment.getContact())
            .viewCount(recruitment.getViewCount())
            .applicantCount(applicantCount)
            .isBookmarked(isBookmarked)
            .isMine(recruitment.isWriter(currentUserId))
            .hasApplied(myApplicationStatus != null)
            .myApplicationStatus(myApplicationStatus)
            .writer(toWriterSummary(recruitment.getWriter(), writerPrimaryRole, writerRegions))
            .createdAt(recruitment.getCreatedAt())
            .updatedAt(recruitment.getUpdatedAt())
            .build();
    }

    public RecruitmentSummary toSummary(
        Recruitment recruitment,
        Long currentUserId,
        boolean isBookmarked,
        LocalDate today
    ) {
        return RecruitmentSummary.builder()
            .id(recruitment.getId())
            .title(recruitment.getTitle())
            .category(recruitment.getCategory())
            .lengthType(recruitment.getLengthType())
            .recruitPart(recruitment.getRecruitPart())
            .location(recruitment.getLocation())
            .pay(recruitment.getPay())
            .deadline(recruitment.getDeadline())
            .dday(calculateDday(recruitment.getDeadline(), today))
            .status(resolveStatus(recruitment.getClosedManually(), recruitment.getDeadline(), today))
            .viewCount(recruitment.getViewCount())
            .isBookmarked(isBookmarked)
            .isMine(recruitment.isWriter(currentUserId))
            .writer(toCardWriterSummary(recruitment.getWriter()))
            .createdAt(recruitment.getCreatedAt())
            .build();
    }

    public MyRecruitmentListResponse.MyRecruitmentSummary toMyRecruitmentSummary(
        Recruitment recruitment,
        Long currentUserId,
        boolean isBookmarked,
        long applicantCount,
        LocalDate today
    ) {
        return MyRecruitmentListResponse.MyRecruitmentSummary.builder()
            .id(recruitment.getId())
            .title(recruitment.getTitle())
            .category(recruitment.getCategory())
            .lengthType(recruitment.getLengthType())
            .recruitPart(recruitment.getRecruitPart())
            .location(recruitment.getLocation())
            .pay(recruitment.getPay())
            .deadline(recruitment.getDeadline())
            .dday(calculateDday(recruitment.getDeadline(), today))
            .status(resolveStatus(recruitment.getClosedManually(), recruitment.getDeadline(), today))
            .viewCount(recruitment.getViewCount())
            .applicantCount(applicantCount)
            .isBookmarked(isBookmarked)
            .isMine(recruitment.isWriter(currentUserId))
            .writer(toCardWriterSummary(recruitment.getWriter()))
            .createdAt(recruitment.getCreatedAt())
            .build();
    }

    public MyApplicationListResponse.MyApplicationSummary toMyApplicationSummary(
        RecruitmentApplication application,
        Long currentUserId,
        boolean isBookmarked,
        LocalDate today
    ) {
        Recruitment recruitment = application.getRecruitment();

        return MyApplicationListResponse.MyApplicationSummary.builder()
            .applicationId(application.getId())
            .applicationStatus(application.getStatus())
            .appliedAt(application.getCreatedAt())
            .id(recruitment.getId())
            .title(recruitment.getTitle())
            .category(recruitment.getCategory())
            .lengthType(recruitment.getLengthType())
            .recruitPart(recruitment.getRecruitPart())
            .location(recruitment.getLocation())
            .pay(recruitment.getPay())
            .deadline(recruitment.getDeadline())
            .dday(calculateDday(recruitment.getDeadline(), today))
            .status(resolveStatus(recruitment.getClosedManually(), recruitment.getDeadline(), today))
            .viewCount(recruitment.getViewCount())
            .isBookmarked(isBookmarked)
            .isMine(recruitment.isWriter(currentUserId))
            .writer(toCardWriterSummary(recruitment.getWriter()))
            .createdAt(recruitment.getCreatedAt())
            .build();
    }

    public RecruitmentApplicantListResponse.ApplicantSummary toApplicantSummary(
        RecruitmentApplication application,
        RoleName primaryRole,
        List<RegionName> regions
    ) {
        Users applicant = application.getUser();

        return RecruitmentApplicantListResponse.ApplicantSummary.builder()
            .applicationId(application.getId())
            .applicationStatus(application.getStatus())
            .message(application.getMessage())
            .referenceLink(application.getReferenceLink())
            .appliedAt(application.getCreatedAt())
            .applicant(RecruitmentApplicantListResponse.ApplicantProfile.builder()
                .id(applicant.getId())
                .nickname(applicant.getNickname())
                .profileImageUrl(applicant.getProfileImageUrl())
                .primaryRole(primaryRole)
                .locations(regions)
                .build())
            .build();
    }

    public RecruitmentApplicationResponse toApplicationResponse(RecruitmentApplication application) {
        return RecruitmentApplicationResponse.builder()
            .applicationId(application.getId())
            .recruitmentId(application.getRecruitment().getId())
            .applicationStatus(application.getStatus())
            .message(application.getMessage())
            .referenceLink(application.getReferenceLink())
            .appliedAt(application.getCreatedAt())
            .build();
    }

    public RecruitmentBookmarkResponse toBookmarkResponse(Long recruitmentId, boolean isBookmarked) {
        return RecruitmentBookmarkResponse.builder()
            .recruitmentId(recruitmentId)
            .isBookmarked(isBookmarked)
            .build();
    }

    public RecruitmentListResponse toListResponse(
        List<RecruitmentSummary> items,
        Long nextCursor,
        Boolean hasNext
    ) {
        return RecruitmentListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    public RecruitmentRecommendationResponse toRecommendationResponse(List<RecruitmentSummary> items) {
        return RecruitmentRecommendationResponse.builder()
            .items(items)
            .build();
    }

    public MyRecruitmentListResponse toMyRecruitmentListResponse(
        List<MyRecruitmentListResponse.MyRecruitmentSummary> items,
        Long nextCursor,
        Boolean hasNext
    ) {
        return MyRecruitmentListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    public MyApplicationListResponse toMyApplicationListResponse(
        List<MyApplicationListResponse.MyApplicationSummary> items,
        Long nextCursor,
        Boolean hasNext
    ) {
        return MyApplicationListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    public RecruitmentApplicantListResponse toApplicantListResponse(
        List<RecruitmentApplicantListResponse.ApplicantSummary> items,
        Long nextCursor,
        Boolean hasNext
    ) {
        return RecruitmentApplicantListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    // 상세 배지, 목록 필터, 마감임박순 그룹 판정, 지원 마감 판정이 전부 이 한 정의를 쓴다.
    // 목록 쿼리의 JPQL 조건식과 판정이 어긋나면 필터 결과와 배지가 불일치한다.
    public RecruitmentStatus resolveStatus(Boolean closedManually, LocalDate deadline, LocalDate today) {
        if (Boolean.TRUE.equals(closedManually)) {
            return RecruitmentStatus.CLOSED;
        }
        if (deadline != null && deadline.isBefore(today)) {
            return RecruitmentStatus.CLOSED;
        }

        return RecruitmentStatus.RECRUITING;
    }

    private RecruitmentDetailResponse.WriterSummary toWriterSummary(
        Users writer,
        RoleName primaryRole,
        List<RegionName> regions
    ) {
        return RecruitmentDetailResponse.WriterSummary.builder()
            .id(writer.getId())
            .nickname(writer.getNickname())
            .profileImageUrl(writer.getProfileImageUrl())
            .primaryRole(primaryRole)
            .locations(regions)
            .build();
    }

    private RecruitmentSummary.WriterSummary toCardWriterSummary(Users writer) {
        return RecruitmentSummary.WriterSummary.builder()
            .id(writer.getId())
            .nickname(writer.getNickname())
            .profileImageUrl(writer.getProfileImageUrl())
            .build();
    }

    private Integer calculateDday(LocalDate deadline, LocalDate today) {
        if (deadline == null) {
            return null;
        }

        return (int) ChronoUnit.DAYS.between(today, deadline);
    }
}
