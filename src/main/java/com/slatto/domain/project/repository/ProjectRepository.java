package com.slatto.domain.project.repository;

import com.slatto.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    // 완료 전환을 조건부 UPDATE 로 선점한다. 동시에 완료를 눌러도 한 요청만 1을 받는다.
    // 조회 후 분기로 처리하면 두 요청이 모두 통과해 참여자 포트폴리오가 두 벌 생긴다.
    //
    // clearAutomatically 는 쓰지 않는다. 호출 시점에 Project 는 이미 로딩되어 변경된 상태라
    // 영속성 컨텍스트를 비우면 그 변경이 사라진다. 호출자가 엔티티 상태를 직접 맞춘다.
    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Project p
        SET p.status = com.slatto.domain.project.enums.ProjectStatus.COMPLETED
        WHERE p.id = :id
            AND p.status <> com.slatto.domain.project.enums.ProjectStatus.COMPLETED
            AND p.deletedAt IS NULL
        """)
    int markCompleted(@Param("id") Long id);
}
