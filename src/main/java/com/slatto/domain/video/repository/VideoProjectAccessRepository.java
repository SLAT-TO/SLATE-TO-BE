package com.slatto.domain.video.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VideoProjectAccessRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    public boolean existsByMemberIdAndProjectId(Long memberId, Long projectId) {
        return entityManagerProvider.getObject().createQuery("""
                        select count(member) from ProjectMember member
                        where member.user.id = :memberId
                          and member.project.id = :projectId
                          and member.leftAt is null
                        """, Long.class)
                .setParameter("memberId", memberId)
                .setParameter("projectId", projectId)
                .getSingleResult() > 0;
    }

    public boolean projectExistsById(Long projectId) {
        return entityManagerProvider.getObject().createQuery(
                        "select count(project) from Project project where project.id = :projectId", Long.class)
                .setParameter("projectId", projectId)
                .getSingleResult() > 0;
    }
}
