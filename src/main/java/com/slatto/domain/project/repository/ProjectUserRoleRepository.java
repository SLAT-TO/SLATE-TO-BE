package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectUserRoleRepository extends JpaRepository<ProjectUserRole, Long> {

    List<ProjectUserRole> findAllByProjectMemberId(Long projectMemberId);

    List<ProjectUserRole> findAllByProjectMemberIdIn(Collection<Long> projectMemberIds);
}
