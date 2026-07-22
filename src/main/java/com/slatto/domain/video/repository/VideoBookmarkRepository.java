package com.slatto.domain.video.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VideoBookmarkRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

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
