package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectUserRoleRepository extends JpaRepository<ProjectUserRole, Long> {

    List<ProjectUserRole> findAllByProjectMemberId(Long projectMemberId);

    List<ProjectUserRole> findAllByProjectMemberIdIn(Collection<Long> projectMemberIds);

    @Query("""
        select pur
        from ProjectUserRole pur
        join fetch pur.projectMember pm
        where pm.id = :projectMemberId
        order by pur.id asc
        """)
    List<ProjectUserRole> findAllByProjectMemberIdOrderByIdAsc(
        @Param("projectMemberId") Long projectMemberId
    );

    @Query("""
        select pur
        from ProjectUserRole pur
        join fetch pur.projectMember pm
        where pm.id in :projectMemberIds
        order by pm.id asc, pur.id asc
        """)
    List<ProjectUserRole> findAllByProjectMemberIdsOrderByProjectMemberIdAscAndIdAsc(
        @Param("projectMemberIds") Collection<Long> projectMemberIds
    );
}
