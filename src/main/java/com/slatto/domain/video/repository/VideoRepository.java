package com.slatto.domain.video.repository;

import com.slatto.domain.video.entity.Video;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public Optional<Video> findByIdAndProjectId(Long videoId, Long projectId) {
        return entityManagerProvider.getObject().createQuery("""
                        select video from Video video
                        where video.id = :videoId and video.project.id = :projectId
                        """, Video.class)
                .setParameter("videoId", videoId)
                .setParameter("projectId", projectId)
                .getResultStream()
                .findFirst();
    }

    public void delete(Video video) {
        entityManagerProvider.getObject().remove(video);
    }

    public void flush() {
        entityManagerProvider.getObject().flush();
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

    public Map<Long, String> findLatestThumbnailUrlsByProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManagerProvider.getObject().createQuery("""
                        select video.project.id, video.thumbnailUrl from Video video
                        where video.project.id in :projectIds
                          and video.thumbnailUrl is not null
                          and video.id = (
                              select max(subVideo.id) from Video subVideo
                              where subVideo.project.id = video.project.id
                                and subVideo.thumbnailUrl is not null
                          )
                        """, Object[].class)
                .setParameter("projectIds", projectIds)
                .getResultList();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
    }
}
