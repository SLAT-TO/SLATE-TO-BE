package com.slatto.domain.video.repository;

import com.slatto.domain.video.entity.Video;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VideoRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    public List<Video> findByProjectIdAndIdLessThanOrderByIdDesc(Long projectId, Long cursor, int limit) {
        return entityManagerProvider.getObject().createQuery("""
                        select video from Video video
                        where video.project.id = :projectId and video.id < :cursor
                        order by video.id desc
                        """, Video.class)
                .setParameter("projectId", projectId)
                .setParameter("cursor", cursor)
                .setMaxResults(limit)
                .getResultList();
    }
}
