package com.slatto.domain.user.service;

import com.slatto.domain.user.dto.PortfolioCreateRequest;
import com.slatto.domain.user.dto.PortfolioCreateResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final String THUMBNAIL_URL_FORMAT = "https://img.youtube.com/vi/%s/maxresdefault.jpg";

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

    private Users getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.NOT_FOUND));
    }
}
