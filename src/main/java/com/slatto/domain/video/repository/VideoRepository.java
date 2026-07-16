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

    public Video save(Video video) {
        entityManagerProvider.getObject().persist(video);
        return video;
    }

    public boolean existsByProjectIdAndYoutubeVideoId(Long projectId, String youtubeVideoId) {
        return entityManagerProvider.getObject().createQuery("""
                        select count(video) from Video video
                        where video.project.id = :projectId
                          and video.youtubeVideoId = :youtubeVideoId
                        """, Long.class)
                .setParameter("projectId", projectId)
                .setParameter("youtubeVideoId", youtubeVideoId)
                .getSingleResult() > 0;
    }

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
