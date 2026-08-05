package com.slatto.domain.user.repository;

import com.slatto.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findAllByUserIdOrderByIdAsc(Long userId);

    // 목록에서 유저별 대표 역할을 한 번에 가져온다. 엔티티가 아니라 (userId, roleName) 튜플만 뽑아
    // users 테이블 로딩과 LAZY 프록시 초기화를 피한다. 정렬은 단건 조회의 "id ASC 첫 행" 규칙과 같다.
    @Query("""
        select ur.user.id, ur.roleName
        from UserRole ur
        where ur.user.id in :userIds
        order by ur.user.id asc, ur.id asc
        """)
    List<Object[]> findRoleRowsByUserIds(@Param("userIds") Collection<Long> userIds);

    void deleteByUserId(Long userId);
}
