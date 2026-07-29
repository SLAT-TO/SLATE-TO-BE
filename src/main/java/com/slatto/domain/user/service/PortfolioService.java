package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
import com.slatto.domain.user.dto.PortfolioDetailResponse;
import com.slatto.domain.user.dto.PortfolioListResponse;
import com.slatto.domain.user.dto.PortfolioSummaryResponse;
import com.slatto.domain.user.dto.PortfolioUpdateRequest;
import com.slatto.domain.user.dto.PortfolioUpdateResponse;
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
            extractThumbnailUrl(request.getYoutubeUrl())
        );
        UserPortfolio savedPortfolio = userPortfolioRepository.save(portfolio);

        replaceRoles(savedPortfolio, user, request.getRoles());

        return PortfolioCreateResponse.builder()
            .id(savedPortfolio.getId())
            .thumbnailUrl(savedPortfolio.getThumbnailUrl())
            .createdAt(savedPortfolio.getCreatedAt())
            .build();
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
            .createdAt(portfolio.getCreatedAt())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }

    public PortfolioListResponse getPortfolios(Long userId, Long cursor, int size) {
        getUserOrThrow(userId);

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

    private String extractThumbnailUrl(String youtubeUrl) {
        return THUMBNAIL_URL_FORMAT.formatted(youtubeUrlParser.extractVideoId(youtubeUrl));
    }

    private String resolveCustomTypeName(CategoryName type, String customTypeName) {
        return type == CategoryName.ETC ? customTypeName : null;
    }

    private String resolveClientName(Kind kind, String clientName) {
        return kind == Kind.EXTERNAL ? clientName : null;
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
