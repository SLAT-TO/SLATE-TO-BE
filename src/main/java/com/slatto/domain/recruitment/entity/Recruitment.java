package com.slatto.domain.recruitment.entity;

import com.slatto.domain.common.entity.BaseEntity;
import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.enums.RecruitmentStatus;
import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruitment")
// 정적 UPDATE 면 수정 시 변경되지 않은 view_count 까지 SET 절에 실려 벌크 업데이트로 증가한 조회수를 덮어쓴다.
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Users writer;

    @Column(name = "title", nullable = true, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = true)
    private CategoryName category;

    @Enumerated(EnumType.STRING)
    @Column(name = "length_type", nullable = true)
    private LengthType lengthType;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruit_part", nullable = true)
    private RoleName recruitPart;

    @Column(name = "shooting_period", nullable = true, length = 255)
    private String shootingPeriod;

    @Column(name = "pay", nullable = true, length = 255)
    private String pay;

    @Column(name = "contact", nullable = true, length = 255)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "location", nullable = true)
    private RegionName location;

    @Column(name = "deadline", nullable = true)
    private LocalDate deadline;

    @Column(name = "closed_manually", nullable = false)
    private Boolean closedManually;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    private Recruitment(
        Users writer,
        String title,
        CategoryName category,
        LengthType lengthType,
        RoleName recruitPart,
        RegionName location,
        String shootingPeriod,
        String pay,
        String contact,
        String description,
        LocalDate deadline
    ) {
        this.writer = writer;
        this.title = title;
        this.category = category;
        this.lengthType = lengthType;
        this.recruitPart = recruitPart;
        this.location = location;
        this.shootingPeriod = shootingPeriod;
        this.pay = pay;
        this.contact = contact;
        this.description = description;
        this.deadline = deadline;
        this.closedManually = false;
        this.viewCount = 0;
    }

    public static Recruitment create(
        Users writer,
        String title,
        CategoryName category,
        LengthType lengthType,
        RoleName recruitPart,
        RegionName location,
        String shootingPeriod,
        String pay,
        String contact,
        String description,
        LocalDate deadline
    ) {
        return new Recruitment(
            writer,
            title,
            category,
            lengthType,
            recruitPart,
            location,
            shootingPeriod,
            pay,
            contact,
            description,
            deadline
        );
    }

    public void update(
        String title,
        CategoryName category,
        LengthType lengthType,
        RoleName recruitPart,
        RegionName location,
        String shootingPeriod,
        String pay,
        String contact,
        String description,
        LocalDate deadline,
        RecruitmentStatus status
    ) {
        if (title != null) {
            this.title = title;
        }
        if (category != null) {
            this.category = category;
        }
        if (lengthType != null) {
            this.lengthType = lengthType;
        }
        if (recruitPart != null) {
            this.recruitPart = recruitPart;
        }
        if (location != null) {
            this.location = location;
        }
        if (shootingPeriod != null) {
            this.shootingPeriod = shootingPeriod;
        }
        if (pay != null) {
            this.pay = pay;
        }
        if (contact != null) {
            this.contact = contact;
        }
        if (description != null) {
            this.description = description;
        }
        if (deadline != null) {
            this.deadline = deadline;
        }
        if (status != null) {
            this.closedManually = (status == RecruitmentStatus.CLOSED);
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isWriter(Long userId) {
        return this.writer.getId().equals(userId);
    }
}
