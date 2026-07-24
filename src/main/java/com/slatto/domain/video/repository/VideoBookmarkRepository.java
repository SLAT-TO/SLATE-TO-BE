package com.slatto.domain.video.repository;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.entity.VideoBookmark;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VideoBookmarkRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    public Optional<VideoBookmark> findByVideoIdAndUserId(Long videoId, Long userId) {
        return entityManagerProvider.getObject().createQuery("""
                        select bookmark from VideoBookmark bookmark
                        where bookmark.video.id = :videoId and bookmark.user.id = :userId
                        """, VideoBookmark.class)
                .setParameter("videoId", videoId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    public void save(Video video, Long userId) {
        EntityManager entityManager = entityManagerProvider.getObject();
        Users user = entityManager.getReference(Users.class, userId);
        entityManager.persist(VideoBookmark.create(video, user));
    }

    public void delete(VideoBookmark bookmark) {
        entityManagerProvider.getObject().remove(bookmark);
    }

    public List<Long> findBookmarkedVideoIdsByUserIdAndVideoIds(Long userId, List<Long> videoIds) {
        return entityManagerProvider.getObject().createQuery("""
                        select bookmark.video.id from VideoBookmark bookmark
                        where bookmark.user.id = :userId and bookmark.video.id in :videoIds
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("videoIds", videoIds)
                .getResultList();
    }
}
