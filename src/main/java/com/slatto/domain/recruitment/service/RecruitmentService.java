package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.RecruitmentCreateRequest;
import com.slatto.domain.recruitment.dto.RecruitmentDetailResponse;
import com.slatto.domain.recruitment.dto.RecruitmentUpdateRequest;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.recruitment.entity.RecruitmentApplication;
import com.slatto.domain.recruitment.enums.RecruitmentApplicationStatus;
import com.slatto.domain.recruitment.repository.RecruitmentApplicationRepository;
import com.slatto.domain.recruitment.repository.RecruitmentBookmarkRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.domain.user.entity.Location;
import com.slatto.domain.user.entity.UserRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.LocationRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.user.repository.UserRoleRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

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
            getUserRegion(currentUserId)
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
            getUserRegion(writerId)
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

    private RegionName getUserRegion(Long userId) {
        return locationRepository.findFirstByUserIdAndRecruitmentIsNullOrderByIdAsc(userId)
            .map(Location::getRegionName)
            .orElse(null);
    }
}
