package com.slatto.domain.user.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_portfolio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CategoryName type;

    @Column(name = "custom_type_name", nullable = true, length = 100)
    private String customTypeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private Kind kind;

    @Column(name = "client_name", nullable = true, length = 255)
    private String clientName;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "comment", nullable = true, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "youtube_url", nullable = true, length = 500)
    private String youtubeUrl;

    @Column(name = "thumbnail_url", nullable = true, length = 500)
    private String thumbnailUrl;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private UserPortfolio(
        Users user,
        String title,
        CategoryName type,
        String customTypeName,
        Kind kind,
        String clientName,
        String description,
        String comment,
        String youtubeUrl,
        String thumbnailUrl
    ) {
        this.user = user;
        this.title = title;
        this.type = type;
        this.customTypeName = customTypeName;
        this.kind = kind;
        this.clientName = clientName;
        this.description = description;
        this.comment = comment;
        this.youtubeUrl = youtubeUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static UserPortfolio create(
        Users user,
        String title,
        CategoryName type,
        String customTypeName,
        Kind kind,
        String clientName,
        String description,
        String comment,
        String youtubeUrl,
        String thumbnailUrl
    ) {
        return new UserPortfolio(
            user,
            title,
            type,
            customTypeName,
            kind,
            clientName,
            description,
            comment,
            youtubeUrl,
            thumbnailUrl
        );
    }

    public void updateBasicInfo(String title, String description, String comment) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (comment != null) {
            this.comment = comment;
        }
    }

    public void changeType(CategoryName type, String customTypeName) {
        this.type = type;
        this.customTypeName = customTypeName;
    }

    public void changeKind(Kind kind, String clientName) {
        this.kind = kind;
        this.clientName = clientName;
    }

    public void changeVideo(String youtubeUrl, String thumbnailUrl) {
        this.youtubeUrl = youtubeUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
