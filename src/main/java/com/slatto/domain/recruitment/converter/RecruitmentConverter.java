package com.slatto.domain.recruitment.converter;

import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RecruitmentConverter {

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
        boolean hasApplied,
        RoleName writerPrimaryRole,
        RegionName writerRegion
    ) {
        // 현재 날짜 참조를 이 한 줄로 몰아둔다. 타임존을 고칠 때 수정 지점이 여기 하나다.
        LocalDate today = LocalDate.now();

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
            .hasApplied(hasApplied)
            .writer(toWriterSummary(recruitment.getWriter(), writerPrimaryRole, writerRegion))
            .createdAt(recruitment.getCreatedAt())
            .updatedAt(recruitment.getUpdatedAt())
            .build();
    }

    private RecruitmentDetailResponse.WriterSummary toWriterSummary(
        Users writer,
        RoleName primaryRole,
        RegionName region
    ) {
        return RecruitmentDetailResponse.WriterSummary.builder()
            .id(writer.getId())
            .nickname(writer.getNickname())
            .profileImageUrl(writer.getProfileImageUrl())
            .primaryRole(primaryRole)
            .location(region)
            .build();
    }

    private Integer calculateDday(LocalDate deadline, LocalDate today) {
        if (deadline == null) {
            return null;
        }

        return (int) ChronoUnit.DAYS.between(today, deadline);
    }

    private RecruitmentStatus resolveStatus(Boolean closedManually, LocalDate deadline, LocalDate today) {
        if (Boolean.TRUE.equals(closedManually)) {
            return RecruitmentStatus.CLOSED;
        }
        if (deadline != null && deadline.isBefore(today)) {
            return RecruitmentStatus.CLOSED;
        }

        return RecruitmentStatus.RECRUITING;
    }
}
