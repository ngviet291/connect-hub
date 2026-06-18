package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.PostHashtag;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtag.PostHashtagId> {

    @Query("""
            SELECT COUNT(ph) > 0
            FROM PostHashtag ph
            WHERE ph.post.id = :postId
            AND ph.hashtag.id = :hashtagId
        """)
    boolean existsByPostIdAndHashtagId(@Param("postId") UUID postId, @Param("hashtagId") UUID hashtagId);

    @Query("""
            SELECT ph
            FROM PostHashtag ph
            JOIN FETCH ph.post p
            WHERE ph.hashtag.id = :hashtagId
            AND p.isDeleted = false
            AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
        """)
    List<PostHashtag> findPostsByHashtagId(@Param("hashtagId") UUID hashtagId,
                                           @Param("cursor") UUID cursor,
                                           Limit limit);
}