package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Post;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByParentPostIdAndIsDeletedFalse(UUID parentPostId);

    @Query("""
            SELECT p
            FROM Post p
            WHERE p.parentPost.id = :parentPostId
            AND p.isDeleted = false
            AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
        """)
    List<Post> findRepliesByParentPostId(@Param("parentPostId") UUID parentPostId,
                                         @Param("cursor") UUID cursor,
                                         Limit limit);

    @Query("""
            SELECT p
            FROM Post p
            WHERE p.visibility = 'PUBLIC'
            AND p.isDeleted = false
            AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
        """)
    List<Post> findPublicFeed(@Param("cursor") UUID cursor, Limit limit);
}