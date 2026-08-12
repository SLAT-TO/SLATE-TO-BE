package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
import com.slatto.domain.user.dto.PortfolioDetailResponse;
import com.slatto.domain.user.dto.PortfolioListResponse;
import com.slatto.domain.user.dto.PortfolioSummaryResponse;
import com.slatto.domain.user.dto.PortfolioUpdateRequest;
import com.slatto.domain.user.dto.PortfolioUpdateResponse;
import com.slatto.domain.user.dto.ProjectPortfolioCreateCommand;
import com.slatto.domain.user.entity.UserPortfolio;
import com.slatto.domain.user.entity.UserPortfolioRole;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import com.slatto.domain.user.repository.UserPortfolioRepository;
import com.slatto.domain.user.repository.UserPortfolioRoleRepository;
import com.slatto.domain.user.repository.UserRepository;
import com.slatto.domain.video.util.YoutubeUrlParser;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String THUMBNAIL_URL_FORMAT = "https://img.youtube.com/vi/%s/hqdefault.jpg";

    private final UserRepository userRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final UserPortfolioRoleRepository userPortfolioRoleRepository;
    private final YoutubeUrlParser youtubeUrlParser;

    @Transactional
    public PortfolioCreateResponse createPortfolio(Long userId, PortfolioCreateRequest request) {
        Users user = getUserOrThrow(userId);

        UserPortfolio portfolio = UserPortfolio.create(
            user,
            request.getTitle(),
            request.getType(),
            resolveCustomTypeName(request.getType(), request.getCustomTypeName()),
            request.getKind(),
            resolveClientName(request.getKind(), request.getClientName()),
            request.getDescription(),
            request.getComment(),
            request.getYoutubeUrl(),
            extractThumbnailUrl(request.getYoutubeUrl()),
            request.getStartDate(),
            request.getEndDate()
        );
        UserPortfolio savedPortfolio = userPortfolioRepository.save(portfolio);

        replaceRoles(savedPortfolio, user, request.getRoles());

        return PortfolioCreateResponse.builder()
            .id(savedPortfolio.getId())
            .thumbnailUrl(savedPortfolio.getThumbnailUrl())
            .createdAt(savedPortfolio.getCreatedAt())
            .build();
    }

    // 프로젝트가 완료로 전환될 때 참여자 전원의 포트폴리오를 한 번에 만든다.
    // 프로젝트에는 대표 영상 개념이 없어 영상 링크는 비우고, 썸네일만 최신 영상에서 가져온다.
    // 링크는 각자 프로필에서 채우고, 채우는 순간 그 링크에서 뽑은 썸네일로 교체된다.
    // 유형이 ETC 여도 프로젝트에는 기타 유형명이 없어 비워두고, 필요하면 본인이 수정한다.
    @Transactional
    public void createProjectPortfolios(ProjectPortfolioCreateCommand command) {
        List<UserPortfolioRole> roles = new ArrayList<>();

        for (ProjectPortfolioCreateCommand.Participant participant : command.getParticipants()) {
            UserPortfolio portfolio = userPortfolioRepository.save(UserPortfolio.create(
                participant.getUser(),
                command.getTitle(),
                command.getType(),
                null,
                command.getKind(),
                resolveClientName(command.getKind(), command.getClientName()),
                command.getDescription(),
                null,
                null,
                command.getThumbnailUrl(),
                command.getStartDate(),
                command.getEndDate()
            ));

            participant.getRoles()
                .stream()
                .distinct()
                .map(roleName -> UserPortfolioRole.create(portfolio, participant.getUser(), roleName))
                .forEach(roles::add);
        }

        userPortfolioRoleRepository.saveAll(roles);
    }

    @Transactional
    public PortfolioUpdateResponse updatePortfolio(
        Long userId,
        Long portfolioId,
        PortfolioUpdateRequest request
    ) {
        UserPortfolio portfolio = getOwnedPortfolioOrThrow(userId, portfolioId);

        portfolio.updateBasicInfo(request.getTitle(), request.getDescription(), request.getComment());

        if (request.getType() != null || request.getCustomTypeName() != null) {
            CategoryName type = request.getType() != null ? request.getType() : portfolio.getType();
            String customTypeName = request.getCustomTypeName() != null
                ? request.getCustomTypeName()
                : portfolio.getCustomTypeName();

            portfolio.changeType(type, resolveCustomTypeName(type, customTypeName));
        }

        if (request.getKind() != null || request.getClientName() != null) {
            Kind kind = request.getKind() != null ? request.getKind() : portfolio.getKind();
            String clientName = request.getClientName() != null
                ? request.getClientName()
                : portfolio.getClientName();

            portfolio.changeKind(kind, resolveClientName(kind, clientName));
        }

        if (request.getYoutubeUrl() != null) {
            portfolio.changeVideo(request.getYoutubeUrl(), extractThumbnailUrl(request.getYoutubeUrl()));
        }

        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : portfolio.getStartDate();
            LocalDate endDate = request.getEndDate() != null
                ? request.getEndDate()
                : portfolio.getEndDate();

            portfolio.changePeriod(startDate, endDate);
        }

        if (request.getRoles() != null) {
            userPortfolioRoleRepository.deleteByPortfolioId(portfolioId);
            userPortfolioRoleRepository.flush();

            replaceRoles(portfolio, portfolio.getUser(), request.getRoles());
            portfolio.markUpdated();
        }

        userPortfolioRepository.flush();

        return PortfolioUpdateResponse.builder()
            .id(portfolio.getId())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }

    @Transactional
    public void deletePortfolio(Long userId, Long portfolioId) {
        UserPortfolio portfolio = getOwnedPortfolioOrThrow(userId, portfolioId);

        portfolio.delete();
    }

    public PortfolioDetailResponse getPortfolio(Long userId, Long portfolioId) {
        UserPortfolio portfolio = getOwnedPortfolioOrThrow(userId, portfolioId);

        List<RoleName> roles = userPortfolioRoleRepository.findAllByPortfolioIdOrderByIdAsc(portfolioId)
            .stream()
            .map(UserPortfolioRole::getRoleName)
            .toList();

        return PortfolioDetailResponse.builder()
            .id(portfolio.getId())
            .title(portfolio.getTitle())
            .type(portfolio.getType())
            .customTypeName(portfolio.getCustomTypeName())
            .kind(portfolio.getKind())
            .clientName(portfolio.getClientName())
            .roles(roles)
            .description(portfolio.getDescription())
            .comment(portfolio.getComment())
            .youtubeUrl(portfolio.getYoutubeUrl())
            .thumbnailUrl(portfolio.getThumbnailUrl())
            .startDate(portfolio.getStartDate())
            .endDate(portfolio.getEndDate())
            .createdAt(portfolio.getCreatedAt())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }

    public PortfolioListResponse getPortfolios(Long userId, Long cursor, int size) {
        getUserOrThrow(userId);

        return getPortfoliosOf(userId, cursor, size);
    }

    // 탈퇴 여부를 판정하지 않는다. getPortfolios 를 쓰면 목록에는 보이는 지원이 상세에서만 404 가 된다.
    // 탈퇴 시 포트폴리오도 함께 soft delete 되므로 탈퇴 유저의 이력 자체는 빈 목록으로 나간다.
    // 닉네임·자기소개를 익명화하는 탈퇴 정책과 맞춘 동작이다.
    public PortfolioListResponse getPortfoliosOf(Long userId, Long cursor, int size) {
        int pageSize = normalizePageSize(size);
        List<UserPortfolio> portfolios = userPortfolioRepository.findActivePortfoliosByCursor(
            userId,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = portfolios.size() > pageSize;
        List<UserPortfolio> currentPagePortfolios = portfolios.stream()
            .limit(pageSize)
            .toList();

        Map<Long, List<RoleName>> rolesByPortfolioId = findRolesByPortfolioIds(
            currentPagePortfolios.stream().map(UserPortfolio::getId).toList()
        );

        List<PortfolioSummaryResponse> items = currentPagePortfolios.stream()
            .map(portfolio -> PortfolioSummaryResponse.builder()
                .id(portfolio.getId())
                .title(portfolio.getTitle())
                .type(portfolio.getType())
                .customTypeName(portfolio.getCustomTypeName())
                .roles(rolesByPortfolioId.getOrDefault(portfolio.getId(), List.of()))
                .thumbnailUrl(portfolio.getThumbnailUrl())
                .startDate(portfolio.getStartDate())
                .endDate(portfolio.getEndDate())
                .createdAt(portfolio.getCreatedAt())
                .build())
            .toList();

        Long nextCursor = hasNext && !items.isEmpty()
            ? items.get(items.size() - 1).getId()
            : null;

        return PortfolioListResponse.builder()
            .items(items)
            .nextCursor(nextCursor)
            .hasNext(hasNext)
            .build();
    }

    private Map<Long, List<RoleName>> findRolesByPortfolioIds(List<Long> portfolioIds) {
        if (portfolioIds.isEmpty()) {
            return Map.of();
        }

        return userPortfolioRoleRepository.findAllByPortfolioIdInOrderByIdAsc(portfolioIds)
            .stream()
            .collect(Collectors.groupingBy(
                role -> role.getPortfolio().getId(),
                Collectors.mapping(UserPortfolioRole::getRoleName, Collectors.toList())
            ));
    }

    private void replaceRoles(UserPortfolio portfolio, Users user, List<RoleName> roleNames) {
        List<UserPortfolioRole> roles = roleNames.stream()
            .distinct()
            .map(roleName -> UserPortfolioRole.create(portfolio, user, roleName))
            .toList();

        userPortfolioRoleRepository.saveAll(roles);
    }

    // 이력의 링크는 유튜브로 제한하지 않는다. 화면 안내도 "Youtube 또는 외부 링크"다.
    // 유튜브가 아니면 썸네일만 비우고 링크는 그대로 저장한다. 파서가 던지는 400 을 그대로
    // 흘려보내면 비메오·네이버TV 를 넣은 사용자가 이유를 알 수 없는 저장 실패만 보게 된다.
    private String extractThumbnailUrl(String youtubeUrl) {
        try {
            return THUMBNAIL_URL_FORMAT.formatted(youtubeUrlParser.extractVideoId(youtubeUrl));
        } catch (BaseException exception) {
            return null;
        }
    }

    private String resolveCustomTypeName(CategoryName type, String customTypeName) {
        return type == CategoryName.ETC ? customTypeName : null;
    }

    // 개인 작업으로 명시한 경우에만 의뢰자를 비운다.
    // kind 가 선택 입력이라, 구분을 고르지 않고 의뢰자만 입력하는 것도 허용한다.
    private String resolveClientName(Kind kind, String clientName) {
        return kind == Kind.PERSONAL ? null : clientName;
    }

    private UserPortfolio getOwnedPortfolioOrThrow(Long userId, Long portfolioId) {
        return userPortfolioRepository.findByIdAndUserIdAndDeletedAtIsNull(portfolioId, userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
