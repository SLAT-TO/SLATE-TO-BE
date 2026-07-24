package com.slatto.domain.video.repository;

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

    public void insertIgnore(Long videoId, Long userId) {
        entityManagerProvider.getObject().createNativeQuery("""
                        insert ignore into video_bookmark (video_id, user_id, created_at, updated_at)
                        values (:videoId, :userId, current_timestamp, current_timestamp)
                        """)
                .setParameter("videoId", videoId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public void deleteByVideoIdAndUserId(Long videoId, Long userId) {
        entityManagerProvider.getObject().createQuery("""
                        delete from VideoBookmark bookmark
                        where bookmark.video.id = :videoId and bookmark.user.id = :userId
                        """)
                .setParameter("videoId", videoId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public long countByVideoIdAndUserId(Long videoId, Long userId) {
        return entityManagerProvider.getObject().createQuery("""
                        select count(bookmark) from VideoBookmark bookmark
                        where bookmark.video.id = :videoId and bookmark.user.id = :userId
                        """, Long.class)
                .setParameter("videoId", videoId)
                .setParameter("userId", userId)
                .getSingleResult();
    }

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
