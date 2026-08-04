package com.slatto.domain.recruitment.repository;

import com.slatto.domain.project.enums.LengthType;
import com.slatto.domain.recruitment.entity.Recruitment;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.RegionName;
import com.slatto.domain.user.enums.RoleName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Optional<Recruitment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.id = :recruitmentId
            and r.deletedAt is null
        """)
    Optional<Recruitment> findActiveWithWriterById(@Param("recruitmentId") Long recruitmentId);

    // 조회수 증가는 updated_at 을 갱신하면 안 되므로 더티 체킹 대신 벌크 업데이트를 쓴다.
    // 본인 공고 제외 조건을 WHERE 절에 넣어, 엔티티 로딩 없이 증가 여부를 판정한다.
    // 반드시 엔티티 로딩보다 먼저 호출할 것 (RecruitmentService.getRecruitment 참조).
    @Modifying
    @Query("""
        update Recruitment r
        set r.viewCount = r.viewCount + 1
        where r.id = :recruitmentId
            and r.deletedAt is null
            and r.writer.id <> :currentUserId
        """)
    int increaseViewCount(
        @Param("recruitmentId") Long recruitmentId,
        @Param("currentUserId") Long currentUserId
    );

    // status 는 컬럼이 아니라 파생값이라 openOnly 로 받아 closed_manually + deadline 으로 전개한다.
    // RecruitmentConverter.resolveStatus 와 판정식이 일치해야 목록 필터와 응답 배지가 어긋나지 않는다.
    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.deletedAt is null
            and (:keyword is null
                or :keyword = ''
                or lower(r.title) like lower(concat('%', :keyword, '%')))
            and (:category is null or r.category = :category)
            and (:lengthType is null or r.lengthType = :lengthType)
            and (:recruitPart is null or r.recruitPart = :recruitPart)
            and (:location is null or r.location = :location)
            and (:openOnly is null
                or (:openOnly = true
                    and r.closedManually = false
                    and (r.deadline is null or r.deadline >= :today))
                or (:openOnly = false
                    and (r.closedManually = true
                        or (r.deadline is not null and r.deadline < :today))))
            and (:cursorId is null or r.id < :cursorId)
        order by r.id desc
        """)
    List<Recruitment> findPageOrderByLatest(
        @Param("keyword") String keyword,
        @Param("category") CategoryName category,
        @Param("lengthType") LengthType lengthType,
        @Param("recruitPart") RoleName recruitPart,
        @Param("location") RegionName location,
        @Param("openOnly") Boolean openOnly,
        @Param("today") LocalDate today,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    // 마감 그룹을 뒤로 보내고 그 안에서 임박순으로 정렬한다.
    // 커서는 (마감그룹, deadline, id) 3단 키로 비교하며 각 키는 order by 와 동일한 순서를 따른다.
    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.deletedAt is null
            and (:keyword is null
                or :keyword = ''
                or lower(r.title) like lower(concat('%', :keyword, '%')))
            and (:category is null or r.category = :category)
            and (:lengthType is null or r.lengthType = :lengthType)
            and (:recruitPart is null or r.recruitPart = :recruitPart)
            and (:location is null or r.location = :location)
            and (:openOnly is null
                or (:openOnly = true
                    and r.closedManually = false
                    and (r.deadline is null or r.deadline >= :today))
                or (:openOnly = false
                    and (r.closedManually = true
                        or (r.deadline is not null and r.deadline < :today))))
            and (
                :cursorId is null
                or (
                    :cursorClosed = false
                    and (r.closedManually = true
                        or (r.deadline is not null and r.deadline < :today))
                )
                or (
                    (
                        (:cursorClosed = false
                            and r.closedManually = false
                            and (r.deadline is null or r.deadline >= :today))
                        or (:cursorClosed = true
                            and (r.closedManually = true
                                or (r.deadline is not null and r.deadline < :today)))
                    )
                    and (
                        (:cursorDeadline is not null
                            and (r.deadline is null
                                or r.deadline > :cursorDeadline
                                or (r.deadline = :cursorDeadline and r.id > :cursorId)))
                        or (:cursorDeadline is null
                            and r.deadline is null
                            and r.id > :cursorId)
                    )
                )
            )
        order by
            case when (r.closedManually = true
                    or (r.deadline is not null and r.deadline < :today))
                then 1 else 0 end asc,
            case when r.deadline is null then 1 else 0 end asc,
            r.deadline asc,
            r.id asc
        """)
    List<Recruitment> findPageOrderByDeadline(
        @Param("keyword") String keyword,
        @Param("category") CategoryName category,
        @Param("lengthType") LengthType lengthType,
        @Param("recruitPart") RoleName recruitPart,
        @Param("location") RegionName location,
        @Param("openOnly") Boolean openOnly,
        @Param("today") LocalDate today,
        @Param("cursorId") Long cursorId,
        @Param("cursorClosed") Boolean cursorClosed,
        @Param("cursorDeadline") LocalDate cursorDeadline,
        Pageable pageable
    );

    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.deletedAt is null
            and (:keyword is null
                or :keyword = ''
                or lower(r.title) like lower(concat('%', :keyword, '%')))
            and (:category is null or r.category = :category)
            and (:lengthType is null or r.lengthType = :lengthType)
            and (:recruitPart is null or r.recruitPart = :recruitPart)
            and (:location is null or r.location = :location)
            and (:openOnly is null
                or (:openOnly = true
                    and r.closedManually = false
                    and (r.deadline is null or r.deadline >= :today))
                or (:openOnly = false
                    and (r.closedManually = true
                        or (r.deadline is not null and r.deadline < :today))))
            and (
                :cursorId is null
                or r.viewCount < :cursorViewCount
                or (r.viewCount = :cursorViewCount and r.id < :cursorId)
            )
        order by r.viewCount desc, r.id desc
        """)
    List<Recruitment> findPageOrderByPopular(
        @Param("keyword") String keyword,
        @Param("category") CategoryName category,
        @Param("lengthType") LengthType lengthType,
        @Param("recruitPart") RoleName recruitPart,
        @Param("location") RegionName location,
        @Param("openOnly") Boolean openOnly,
        @Param("today") LocalDate today,
        @Param("cursorId") Long cursorId,
        @Param("cursorViewCount") Integer cursorViewCount,
        Pageable pageable
    );

    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.writer.id = :currentUserId
            and r.deletedAt is null
            and (:openOnly is null
                or (:openOnly = true
                    and r.closedManually = false
                    and (r.deadline is null or r.deadline >= :today))
                or (:openOnly = false
                    and (r.closedManually = true
                        or (r.deadline is not null and r.deadline < :today))))
            and (:cursorId is null or r.id < :cursorId)
        order by r.id desc
        """)
    List<Recruitment> findMyRecruitmentsByCursor(
        @Param("currentUserId") Long currentUserId,
        @Param("openOnly") Boolean openOnly,
        @Param("today") LocalDate today,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    // 매칭 조건을 exists 로 검사해 유저의 역할/카테고리/지역 컬렉션을 바인딩하지 않는다.
    // in :collection 은 빈 컬렉션이 1=0 으로 렌더링돼 온보딩 미완료 유저가 조용히 빈 결과를 받는다.
    // 가중치 4/2/1 은 2의 거듭제곱이라 역할 > 카테고리 > 지역 우선순위가 동점 없이 유지된다.
    @Query("""
        select r
        from Recruitment r
        join fetch r.writer
        where r.deletedAt is null
            and r.closedManually = false
            and (r.deadline is null or r.deadline >= :today)
            and r.writer.id <> :currentUserId
        order by
            (
                case when exists (
                    select 1
                    from UserRole ur
                    where ur.user.id = :currentUserId
                        and ur.roleName = r.recruitPart
                ) then 4 else 0 end
                +
                case when exists (
                    select 1
                    from UserCategory uc
                    where uc.user.id = :currentUserId
                        and uc.categoryName = r.category
                ) then 2 else 0 end
                +
                case when exists (
                    select 1
                    from Location l
                    where l.user.id = :currentUserId
                        and l.recruitment is null
                        and l.regionName = r.location
                ) then 1 else 0 end
            ) desc,
            r.id desc
        """)
    List<Recruitment> findRecommended(
        @Param("currentUserId") Long currentUserId,
        @Param("today") LocalDate today,
        Pageable pageable
    );
}
