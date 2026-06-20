package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Post;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // ── Fetch with details ────────────────────────────────────────────────────

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

    // ── Increment / Decrement counters (runtime) ──────────────────────────────

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = GREATEST(p.commentCount - 1, 0) WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = p.reactionCount + 1 WHERE p.id = :postId")
    void incrementReactionCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = GREATEST(p.reactionCount - 1, 0) WHERE p.id = :postId")
    void decrementReactionCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.bookmarkCount = p.bookmarkCount + 1 WHERE p.id = :postId")
    void incrementBookmarkCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.bookmarkCount = GREATEST(p.bookmarkCount - 1, 0) WHERE p.id = :postId")
    void decrementBookmarkCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :postId")
    void incrementRepostCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.repostCount = GREATEST(p.repostCount - 1, 0) WHERE p.id = :postId")
    void decrementRepostCount(@Param("postId") UUID postId);

    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") UUID postId);

    // ── Sync counters (init data) ─────────────────────────────────────────────

    @Transactional
    @Modifying
    @Query("""
        UPDATE Post p SET p.reactionCount = (
            SELECT COUNT(r) FROM Reaction r WHERE r.post = p
        )
    """)
    void syncAllReactionCounts();

    @Transactional
    @Modifying
    @Query("""
        UPDATE Post p SET p.commentCount = (
            SELECT COUNT(c) FROM Post c WHERE c.parentPost = p AND c.isDeleted = false
        )
    """)
    void syncAllCommentCounts();

    @Transactional
    @Modifying
    @Query("""
        UPDATE Post p SET p.repostCount = (
            SELECT COUNT(r) FROM Repost r WHERE r.post = p
        )
    """)
    void syncAllRepostCounts();

    @Transactional
    @Modifying
    @Query("""
        UPDATE Post p SET p.bookmarkCount = (
            SELECT COUNT(b) FROM Bookmark b WHERE b.post = p
        )
    """)
    void syncAllBookmarkCounts();

    @Transactional
    @Modifying
    @Query("""
        UPDATE Post p SET p.viewCount = (
            SELECT COUNT(v) FROM PostView v WHERE v.post = p AND v.user IS NOT NULL
        )
    """)
    void syncAllViewCounts();
}