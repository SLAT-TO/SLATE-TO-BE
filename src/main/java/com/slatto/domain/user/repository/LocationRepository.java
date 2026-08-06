package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByUserIdAndRecruitmentIsNullOrderByIdAsc(Long userId);

    void deleteByUserIdAndRecruitmentIsNull(Long userId);

    // findAllByUserIdAndRecruitmentIsNullOrderByIdAsc 의 배치판. 유저당 여러 행이 나온다.
    // recruitment is null 이 "유저 활동 지역"의 정의다(공고 촬영 지역이 아니다).
    @Query("""
        select l.user.id, l.regionName
        from Location l
        where l.user.id in :userIds
            and l.recruitment is null
        order by l.user.id asc, l.id asc
        """)
    List<Object[]> findUserRegionRowsByUserIds(@Param("userIds") Collection<Long> userIds);
}
