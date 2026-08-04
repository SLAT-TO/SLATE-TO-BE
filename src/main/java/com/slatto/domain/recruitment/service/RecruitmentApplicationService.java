package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.MyApplicationListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicantListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationStatusUpdateRequest;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.recruitment.exception.RecruitmentErrorCode;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentBookmarkRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.LocationRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.repository.UserRoleRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final LocationRepository locationRepository;
    private final RecruitmentConverter recruitmentConverter;
    private final RecruitmentNotificationDispatcher recruitmentNotificationDispatcher;

    @Transactional
    public RecruitmentApplicationResponse applyToRecruitment(
        Long currentUserId,
        Long recruitmentId,
        RecruitmentApplicationCreateRequest request
    ) {
        // 알림 수신자와 공고 제목이 필요하고, 작성자 생존 확인도 추가 쿼리 없이 해야 하므로 fetch join 버전을 쓴다.
        Recruitment recruitment = recruitmentRepository.findActiveWithWriterById(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateOpen(recruitment);

        if (recruitment.isWriter(currentUserId)) {
            throw new BaseException(RecruitmentErrorCode.RECRUITMENT_SELF_APPLICATION);
        }

        if (recruitmentApplicationRepository
            .existsByRecruitmentIdAndUserIdAndDeletedAtIsNull(recruitmentId, currentUserId)) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_ALREADY_APPLIED);
        }

        Users applicant = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        RecruitmentApplication saved = recruitmentApplicationRepository.save(
            RecruitmentApplication.create(
                applicant,
                recruitment,
                request.getMessage(),
                request.getReferenceLink()
            )
        );

        dispatchAppliedNotification(recruitment, applicant);

        return recruitmentConverter.toApplicationResponse(saved);
    }

    public RecruitmentApplicantListResponse getApplicants(
        Long currentUserId,
        Long recruitmentId,
        RecruitmentApplicationStatus status,
        Long cursor,
        int size
    ) {
        Recruitment recruitment = recruitmentRepository.findByIdAndDeletedAtIsNull(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateWriter(recruitment, currentUserId);

        int pageSize = normalizePageSize(size);
        List<RecruitmentApplication> applications = recruitmentApplicationRepository.findApplicantsByCursor(
            recruitmentId,
            status,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = applications.size() > pageSize;
        List<RecruitmentApplication> currentPage = applications.stream()
            .limit(pageSize)
            .toList();

        List<Long> applicantIds = currentPage.stream()
            .map(application -> application.getUser().getId())
            .toList();
        Map<Long, RoleName> primaryRoleByUserId = getPrimaryRoleByUserId(applicantIds);
        Map<Long, RegionName> regionByUserId = getRegionByUserId(applicantIds);

        List<RecruitmentApplicantListResponse.ApplicantSummary> items = currentPage.stream()
            .map(application -> recruitmentConverter.toApplicantSummary(
                application,
                primaryRoleByUserId.get(application.getUser().getId()),
                regionByUserId.get(application.getUser().getId())
            ))
            .toList();

        Long nextCursor = hasNext && !currentPage.isEmpty()
            ? currentPage.get(currentPage.size() - 1).getId()
            : null;

        return recruitmentConverter.toApplicantListResponse(items, nextCursor, hasNext);
    }

    @Transactional
    public void changeApplicationStatus(
        Long currentUserId,
        Long recruitmentId,
        Long applicationId,
        RecruitmentApplicationStatusUpdateRequest request
    ) {
        Recruitment recruitment = recruitmentRepository.findByIdAndDeletedAtIsNull(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateWriter(recruitment, currentUserId);

        // recruitmentId 조건이 없으면 다른 공고의 applicationId 로 작성자 검증을 우회할 수 있다.
        RecruitmentApplication application = recruitmentApplicationRepository
            .findByIdAndRecruitmentIdAndDeletedAtIsNull(applicationId, recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        if (request.getStatus() == RecruitmentApplicationStatus.PENDING) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        if (!application.isPending()) {
            throw new BaseException(RecruitmentErrorCode.APPLICATION_ALREADY_HANDLED);
        }

        application.changeStatus(request.getStatus());
    }

    public MyApplicationListResponse getMyApplications(
        Long currentUserId,
        RecruitmentApplicationStatus status,
        Long cursor,
        int size
    ) {
        int pageSize = normalizePageSize(size);
        LocalDate today = recruitmentConverter.currentDate();

        List<RecruitmentApplication> applications = recruitmentApplicationRepository.findMyApplicationsByCursor(
            currentUserId,
            status,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = applications.size() > pageSize;
        List<RecruitmentApplication> currentPage = applications.stream()
            .limit(pageSize)
            .toList();

        List<Long> recruitmentIds = currentPage.stream()
            .map(application -> application.getRecruitment().getId())
            .toList();
        Set<Long> bookmarkedIds = recruitmentIds.isEmpty()
            ? Set.of()
            : Set.copyOf(recruitmentBookmarkRepository.findBookmarkedRecruitmentIds(currentUserId, recruitmentIds));

        List<MyApplicationListResponse.MyApplicationSummary> items = currentPage.stream()
            .map(application -> recruitmentConverter.toMyApplicationSummary(
                application,
                currentUserId,
                bookmarkedIds.contains(application.getRecruitment().getId()),
                today
            ))
            .toList();

        // 커서는 공고 id 가 아니라 지원 id 다.
        Long nextCursor = hasNext && !currentPage.isEmpty()
            ? currentPage.get(currentPage.size() - 1).getId()
            : null;

        return recruitmentConverter.toMyApplicationListResponse(items, nextCursor, hasNext);
    }

    // 마감 판정은 상세 배지/목록 필터와 같은 정의를 써야 한다.
    private void validateOpen(Recruitment recruitment) {
        RecruitmentStatus status = recruitmentConverter.resolveStatus(
            recruitment.getClosedManually(),
            recruitment.getDeadline(),
            recruitmentConverter.currentDate()
        );

        if (status == RecruitmentStatus.CLOSED) {
            throw new BaseException(RecruitmentErrorCode.RECRUITMENT_CLOSED);
        }
    }

    // 탈퇴 작성자나 제목이 빈 공고는 NotificationService 가 예외를 던지므로 호출 자체를 건너뛴다.
    // writer 는 findActiveWithWriterById 의 fetch join 으로 완전 로딩돼 있어 추가 쿼리가 없다.
    private void dispatchAppliedNotification(Recruitment recruitment, Users applicant) {
        Users writer = recruitment.getWriter();

        if (writer.getDeletedAt() != null || !StringUtils.hasText(recruitment.getTitle())) {
            return;
        }

        // 트랜잭션 커밋이 디스패처 프록시에서 일어나므로 예외를 여기서 잡아야 한다.
        // 디스패처가 REQUIRES_NEW 라 실패해도 지원 트랜잭션은 rollback-only 로 마킹되지 않는다.
        try {
            recruitmentNotificationDispatcher.dispatchApplied(
                writer.getId(),
                recruitment.getId(),
                recruitment.getTitle(),
                applicant.getNickname()
            );
        } catch (Exception exception) {
            log.error(
                "[Recruitment] 지원 알림 생성 실패. recruitmentId={}, writerId={}",
                recruitment.getId(),
                writer.getId(),
                exception
            );
        }
    }

    private void validateWriter(Recruitment recruitment, Long currentUserId) {
        if (!recruitment.isWriter(currentUserId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    private Map<Long, RoleName> getPrimaryRoleByUserId(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        // order by id asc + 머지 함수가 단건 조회의 "id ASC 첫 행" 규칙과 같은 값을 만든다.
        return userRoleRepository.findRoleRowsByUserIds(userIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (RoleName) row[1],
                (first, second) -> first
            ));
    }

    private Map<Long, RegionName> getRegionByUserId(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return locationRepository.findUserRegionRowsByUserIds(userIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (RegionName) row[1],
                (first, second) -> first
            ));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
