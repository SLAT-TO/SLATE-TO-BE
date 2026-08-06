package com.slatto.domain.user.entity;

import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = true)
    private Recruitment recruitment;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_name", nullable = false)
    private RegionName regionName;

    private Location(Users user, RegionName regionName) {
        this.user = user;
        this.regionName = regionName;
    }

    public static Location createUserLocation(Users user, RegionName regionName) {
        if (user == null) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }
        return new Location(user, regionName);
    }
}
