package com.slatto.domain.project.service;

import com.slatto.domain.project.dto.ProjectInvitationCreateRequest;
import com.slatto.domain.project.dto.ProjectInvitationCreateResponse;
import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectInvitation;
import com.slatto.domain.project.entity.ProjectMember;
import com.slatto.domain.project.enums.ExpirationPeriod;
import com.slatto.domain.project.repository.ProjectInvitationRepository;
import com.slatto.global.config.properties.ProjectInvitationProperties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectInvitationService {

    private static final ExpirationPeriod DEFAULT_EXPIRATION_PERIOD = ExpirationPeriod.HOURS_72;
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectAccessValidator projectAccessValidator;
    private final ProjectInvitationProperties projectInvitationProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ProjectInvitationCreateResponse createInvitation(
        Long projectId,
        Long currentUserId,
        ProjectInvitationCreateRequest request
    ) {
        Project project = projectAccessValidator.getProjectOrThrow(projectId);
        ProjectMember inviter = projectAccessValidator.getCurrentAdminOrThrow(projectId, currentUserId);

        String token = generateUniqueToken();
        LocalDateTime expiresAt = resolveExpirationPeriod(request).calculateExpiresAt(LocalDateTime.now());

        ProjectInvitation projectInvitation = ProjectInvitation.create(
            project,
            inviter.getUser(),
            hashToken(token),
            expiresAt
        );
        projectInvitationRepository.save(projectInvitation);

        return ProjectInvitationCreateResponse.builder()
            .inviteUrl(toInviteUrl(token))
            .expiresAt(expiresAt)
            .build();
    }

    private ExpirationPeriod resolveExpirationPeriod(ProjectInvitationCreateRequest request) {
        if (request == null || request.getExpirationPeriod() == null) {
            return DEFAULT_EXPIRATION_PERIOD;
        }

        return request.getExpirationPeriod();
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = generateToken();
            if (!projectInvitationRepository.existsByTokenHash(hashToken(token))) {
                return token;
            }
        }

        throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String toInviteUrl(String token) {
        return projectInvitationProperties.baseUrl().replaceAll("/+$", "") + "/" + token;
    }
}
