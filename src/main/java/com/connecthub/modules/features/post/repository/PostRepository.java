package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Post;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.media
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.quotePost qp
    LEFT JOIN FETCH qp.media
    LEFT JOIN FETCH qp.user
    WHERE p.id = :postId
""")
    Optional<Post> findByIdWithDetails(@Param("postId") UUID postId);
    @Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.media
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.quotePost qp
    LEFT JOIN FETCH qp.media
    LEFT JOIN FETCH qp.user
    WHERE p.visibility = 'PUBLIC'
    AND p.isDeleted = false
    AND (:cursor IS NULL OR p.id < :cursor)
    ORDER BY p.id DESC
""")
    List<Post> findPublicFeedWithDetails(@Param("cursor") UUID cursor, Limit limit);

    @Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.media
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.quotePost qp
    LEFT JOIN FETCH qp.media
    LEFT JOIN FETCH qp.user
    WHERE p.parentPost.id = :parentPostId
    AND p.isDeleted = false
    AND (:cursor IS NULL OR p.id < :cursor)
    ORDER BY p.id DESC
""")
    List<Post> findRepliesByParentPostIdWithDetails(
            @Param("parentPostId") UUID parentPostId,
            @Param("cursor") UUID cursor,
            Limit limit);
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = GREATEST(p.commentCount - 1, 0) WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") UUID postId);
}