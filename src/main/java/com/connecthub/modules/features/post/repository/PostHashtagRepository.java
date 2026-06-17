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
            SELECT DISTINCT ph
            FROM PostHashtag ph
            WHERE ph.hashtag.id = :hashtagId
            AND ph.post.isDeleted = false
            AND (:cursor IS NULL OR ph.post.id < :cursor)
            ORDER BY ph.post.id DESC
        """)
    List<PostHashtag> findPostsByHashtagId(@Param("hashtagId") UUID hashtagId,
                                           @Param("cursor") UUID cursor,
                                           Limit limit);
}