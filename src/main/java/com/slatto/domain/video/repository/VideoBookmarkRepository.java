package com.slatto.domain.video.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VideoBookmarkRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    public boolean existsByVideoIdAndUserId(Long videoId, Long userId) {
        return entityManagerProvider.getObject().createQuery("""
                        select count(bookmark) from VideoBookmark bookmark
                        where bookmark.video.id = :videoId and bookmark.user.id = :userId
                        """, Long.class)
                .setParameter("videoId", videoId)
                .setParameter("userId", userId)
                .getSingleResult() > 0;
    }
}
