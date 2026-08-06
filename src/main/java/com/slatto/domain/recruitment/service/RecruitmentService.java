package com.slatto.domain.recruitment.service;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.MyRecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentRecommendationResponse;
import com.slatto.domain.recruitment.dto.RecruitmentSummary;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.enums.RecruitmentSortType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentBookmarkRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Location;
import com.slatto.domain.user.entity.UserRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.LocationRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.repository.UserRoleRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_RECOMMENDATION_SIZE = 10;
    private static final int MAX_RECOMMENDATION_SIZE = 20;

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final LocationRepository locationRepository;
    private final RecruitmentConverter recruitmentConverter;

    @Transactional
    public RecruitmentDetailResponse createRecruitment(Long currentUserId, RecruitmentCreateRequest request) {
        Users writer = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        Recruitment saved = recruitmentRepository.save(recruitmentConverter.toRecruitment(writer, request));

        // 생성 직후라 지원자 수 0, 관심 등록 false, 내 지원 없음(null)이 확정이다. 집계 쿼리를 쏘지 않는다.
        return recruitmentConverter.toDetailResponse(
            saved,
            currentUserId,
            0L,
            false,
            null,
            getPrimaryRole(currentUserId),
            getUserRegions(currentUserId)
        );
    }

    // 조회수 벌크 업데이트가 DML 이라 readOnly 트랜잭션에서는 실행되지 않는다.
    @Transactional
    public RecruitmentDetailResponse getRecruitment(Long currentUserId, Long recruitmentId) {
        // 증가를 먼저 하고 로딩해야 같은 트랜잭션에서 방금 쓴 값을 읽는다(read-your-own-writes).
        // 순서를 뒤집으면 응답 viewCount 가 stale 해진다. 반환값 0(본인 공고/미존재)은 무시하고 404 판정은 아래 SELECT 가 맡는다.
        recruitmentRepository.increaseViewCount(recruitmentId, currentUserId);

        Recruitment recruitment = recruitmentRepository.findActiveWithWriterById(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        return buildDetailResponse(recruitment, currentUserId);
    }

    @Transactional
    public RecruitmentDetailResponse updateRecruitment(
        Long currentUserId,
        Long recruitmentId,
        RecruitmentUpdateRequest request
    ) {
        // 404 를 403 보다 먼저 판정해야 리소스 존재 여부가 새지 않는다.
        Recruitment recruitment = recruitmentRepository.findActiveWithWriterById(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateWriter(recruitment, currentUserId);

        recruitment.update(
            request.getTitle(),
            request.getCategory(),
            request.getLengthType(),
            request.getRecruitPart(),
            request.getLocation(),
            request.getShootingPeriod(),
            request.getPay(),
            request.getContact(),
            request.getDescription(),
            request.getDeadline(),
            request.getStatus()
        );

        return buildDetailResponse(recruitment, currentUserId);
    }

    @Transactional
    public void deleteRecruitment(Long currentUserId, Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findByIdAndDeletedAtIsNull(recruitmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));

        validateWriter(recruitment, currentUserId);

        recruitment.softDelete();
    }

    public RecruitmentListResponse getRecruitments(
        Long currentUserId,
        String keyword,
        CategoryName category,
        LengthType lengthType,
        RoleName recruitPart,
        RegionName location,
        RecruitmentStatus status,
        RecruitmentSortType sort,
        Long cursor,
        int size
    ) {
        int pageSize = normalizePageSize(size);
        // 필터 바인딩과 표시 계산이 같은 날짜를 써야 목록 결과와 status 배지가 어긋나지 않는다.
        LocalDate today = recruitmentConverter.currentDate();
        String normalizedKeyword = normalizeKeyword(keyword);
        Boolean openOnly = toOpenOnly(status);
        RecruitmentSortType sortType = sort == null ? RecruitmentSortType.LATEST : sort;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Recruitment> recruitments = switch (sortType) {
            case LATEST -> recruitmentRepository.findPageOrderByLatest(
                normalizedKeyword, category, lengthType, recruitPart, location, openOnly, today, cursor, pageable
            );
            case DEADLINE -> {
                Recruitment cursorRecruitment = resolveCursorRecruitment(cursor);
                yield recruitmentRepository.findPageOrderByDeadline(
                    normalizedKeyword, category, lengthType, recruitPart, location, openOnly, today, cursor,
                    getCursorClosed(cursorRecruitment, today),
                    cursorRecruitment == null ? null : cursorRecruitment.getDeadline(),
                    pageable
                );
            }
            case POPULAR -> {
                Recruitment cursorRecruitment = resolveCursorRecruitment(cursor);
                yield recruitmentRepository.findPageOrderByPopular(
                    normalizedKeyword, category, lengthType, recruitPart, location, openOnly, today, cursor,
                    cursorRecruitment == null ? null : cursorRecruitment.getViewCount(),
                    pageable
                );
            }
        };

        boolean hasNext = recruitments.size() > pageSize;
        List<Recruitment> currentPage = recruitments.stream()
            .limit(pageSize)
            .toList();
        Set<Long> bookmarkedIds = getBookmarkedRecruitmentIds(currentUserId, currentPage);

        List<RecruitmentSummary> items = currentPage.stream()
            .map(recruitment -> recruitmentConverter.toSummary(
                recruitment,
                currentUserId,
                bookmarkedIds.contains(recruitment.getId()),
                today
            ))
            .toList();

        Long nextCursor = hasNext && !currentPage.isEmpty()
            ? currentPage.get(currentPage.size() - 1).getId()
            : null;

        return recruitmentConverter.toListResponse(items, nextCursor, hasNext);
    }

    // 매칭 가중치는 쿼리가 계산한다. 온보딩 미완료 유저는 전 조건이 0점이라 최신순으로 자동 폴백된다.
    public RecruitmentRecommendationResponse getRecommendedRecruitments(Long currentUserId, int size) {
        LocalDate today = recruitmentConverter.currentDate();
        List<Recruitment> recruitments = recruitmentRepository.findRecommended(
            currentUserId,
            today,
            PageRequest.of(0, normalizeRecommendationSize(size))
        );

        Set<Long> bookmarkedIds = getBookmarkedRecruitmentIds(currentUserId, recruitments);

        List<RecruitmentSummary> items = recruitments.stream()
            .map(recruitment -> recruitmentConverter.toSummary(
                recruitment,
                currentUserId,
                bookmarkedIds.contains(recruitment.getId()),
                today
            ))
            .toList();

        return recruitmentConverter.toRecommendationResponse(items);
    }

    public MyRecruitmentListResponse getMyRecruitments(
        Long currentUserId,
        RecruitmentStatus status,
        Long cursor,
        int size
    ) {
        int pageSize = normalizePageSize(size);
        LocalDate today = recruitmentConverter.currentDate();

        List<Recruitment> recruitments = recruitmentRepository.findMyRecruitmentsByCursor(
            currentUserId,
            toOpenOnly(status),
            today,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = recruitments.size() > pageSize;
        List<Recruitment> currentPage = recruitments.stream()
            .limit(pageSize)
            .toList();

        Set<Long> bookmarkedIds = getBookmarkedRecruitmentIds(currentUserId, currentPage);
        Map<Long, Long> applicantCountById = getApplicantCountByRecruitmentId(currentPage);

        List<MyRecruitmentListResponse.MyRecruitmentSummary> items = currentPage.stream()
            .map(recruitment -> recruitmentConverter.toMyRecruitmentSummary(
                recruitment,
                currentUserId,
                bookmarkedIds.contains(recruitment.getId()),
                applicantCountById.getOrDefault(recruitment.getId(), 0L),
                today
            ))
            .toList();

        Long nextCursor = hasNext && !currentPage.isEmpty()
            ? currentPage.get(currentPage.size() - 1).getId()
            : null;

        return recruitmentConverter.toMyRecruitmentListResponse(items, nextCursor, hasNext);
    }

    // group by 는 지원자 0명인 공고를 결과에서 누락시키므로 호출부에서 getOrDefault 로 채운다.
    private Map<Long, Long> getApplicantCountByRecruitmentId(List<Recruitment> recruitments) {
        List<Long> recruitmentIds = recruitments.stream()
            .map(Recruitment::getId)
            .toList();

        if (recruitmentIds.isEmpty()) {
            return Map.of();
        }

        return recruitmentApplicationRepository.countDistinctApplicantsByRecruitmentIds(recruitmentIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));
    }

    // 커서 행이 그 사이 soft delete 돼도 정렬키(closedManually/deadline/viewCount)는 유효하다.
    // deletedAt 조건을 걸면 삭제 레이스에서 정상 요청이 400 이 된다. 의도적으로 findById 를 쓴다.
    private Recruitment resolveCursorRecruitment(Long cursor) {
        if (cursor == null) {
            return null;
        }

        return recruitmentRepository.findById(cursor)
            .orElseThrow(() -> new BaseException(CommonErrorCode.BAD_REQUEST));
    }

    private Boolean getCursorClosed(Recruitment cursorRecruitment, LocalDate today) {
        if (cursorRecruitment == null) {
            return null;
        }

        return recruitmentConverter.resolveStatus(
            cursorRecruitment.getClosedManually(),
            cursorRecruitment.getDeadline(),
            today
        ) == RecruitmentStatus.CLOSED;
    }

    // status 는 파생값이라 컬럼 비교가 불가능하다. 쿼리가 closed_manually 와 deadline 으로 전개한다.
    private Boolean toOpenOnly(RecruitmentStatus status) {
        return status == null ? null : status == RecruitmentStatus.RECRUITING;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private Set<Long> getBookmarkedRecruitmentIds(Long currentUserId, List<Recruitment> recruitments) {
        List<Long> recruitmentIds = recruitments.stream()
            .map(Recruitment::getId)
            .toList();

        if (recruitmentIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(
            recruitmentBookmarkRepository.findBookmarkedRecruitmentIds(currentUserId, recruitmentIds)
        );
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int normalizeRecommendationSize(int size) {
        if (size <= 0) {
            return DEFAULT_RECOMMENDATION_SIZE;
        }

        return Math.min(size, MAX_RECOMMENDATION_SIZE);
    }

    private RecruitmentDetailResponse buildDetailResponse(Recruitment recruitment, Long currentUserId) {
        Long recruitmentId = recruitment.getId();
        Long writerId = recruitment.getWriter().getId();

        return recruitmentConverter.toDetailResponse(
            recruitment,
            currentUserId,
            recruitmentApplicationRepository.countDistinctApplicants(recruitmentId),
            recruitmentBookmarkRepository.existsByRecruitmentIdAndUserId(recruitmentId, currentUserId),
            getMyApplicationStatus(recruitmentId, currentUserId),
            getPrimaryRole(writerId),
            getUserRegions(writerId)
        );
    }

    // hasApplied 와 myApplicationStatus 를 한 번의 조회로 채운다. 미지원이면 null 이다.
    private RecruitmentApplicationStatus getMyApplicationStatus(Long recruitmentId, Long currentUserId) {
        return recruitmentApplicationRepository
            .findFirstByRecruitmentIdAndUserIdAndDeletedAtIsNullOrderByIdDesc(recruitmentId, currentUserId)
            .map(RecruitmentApplication::getStatus)
            .orElse(null);
    }

    private void validateWriter(Recruitment recruitment, Long currentUserId) {
        if (!recruitment.isWriter(currentUserId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
    }

    // 온보딩 미완료 유저는 user_role 이 0건이라 isEmpty 가드가 없으면 500 이 난다.
    private RoleName getPrimaryRole(Long userId) {
        List<UserRole> roles = userRoleRepository.findAllByUserIdOrderByIdAsc(userId);

        return roles.isEmpty() ? null : roles.get(0).getRoleName();
    }

    private List<RegionName> getUserRegions(Long userId) {
        return locationRepository.findAllByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .stream()
            .map(Location::getRegionName)
            .toList();
    }
}
