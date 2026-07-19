package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    boolean existsByTokenHash(String tokenHash);

    @Query("""
        select pi
        from ProjectInvitation pi
        join fetch pi.project p
        join fetch pi.inviter i
        left join fetch pi.accepter a
        where pi.tokenHash = :tokenHash
        """)
    Optional<ProjectInvitation> findByTokenHashWithProjectAndUsers(
        @Param("tokenHash") String tokenHash
    );
}
