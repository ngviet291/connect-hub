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

    // Fetch đầy đủ 1 post theo ID (không có pagination nên không bị lỗi multiple bags)
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.media
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.quotePost qp
        LEFT JOIN FETCH qp.media
        LEFT JOIN FETCH qp.user
        LEFT JOIN FETCH p.postHashtags ph
        LEFT JOIN FETCH ph.hashtag
        LEFT JOIN FETCH p.mentions m
        LEFT JOIN FETCH m.user
        WHERE p.id = :postId
        AND p.isDeleted = false
    """)
    Optional<Post> findByIdWithDetails(@Param("postId") UUID postId);
    /**
     * KHÔNG DÙNG ORDER BY TRONG JPQL ĐƯỢC: JPQL thuần túy không hỗ trợ sắp xếp theo thứ tự của list `:ids` truyền vào.
     * DB SẼ TRẢ VỀ THỨ TỰ LỘN XỘN: Câu lệnh `WHERE p.id IN :ids` sẽ khiến DB trả dữ liệu về theo thứ tự quét Index ngẫu nhiên.
     * KHÔNG PHÂN TRANG Ở ĐÂY: Hàm này chỉ nhận IDs đã phân trang từ trước để tránh lỗi MultipleBagFetchException.
     * Thứ tự sắp xếp đúng chuẩn của Cursor sẽ được xử lý ở tầng Service bằng Java Stream.
     */
    // Fetch đầy đủ nhiều post theo danh sách IDs (không có pagination)
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.media
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.quotePost qp
        LEFT JOIN FETCH qp.media
        LEFT JOIN FETCH qp.user
        LEFT JOIN FETCH p.postHashtags ph
        LEFT JOIN FETCH ph.hashtag
        LEFT JOIN FETCH p.mentions m
        LEFT JOIN FETCH m.user
        WHERE p.id IN :ids
    """)
    List<Post> findAllWithDetailsByIds(@Param("ids") List<UUID> ids);

    // Query 1: chỉ lấy IDs cho feed (có pagination, không JOIN collection)
    @Query("""
        SELECT p.id FROM Post p
        WHERE p.visibility = 'PUBLIC'
        AND p.isDeleted = false
        AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.id DESC
    """)
    List<UUID> findPublicFeedIds(@Param("cursor") UUID cursor, Limit limit);

    // Query 2: chỉ lấy IDs của replies (có pagination, không JOIN collection)
    @Query("""
        SELECT p.id FROM Post p
        WHERE p.parentPost.id = :parentPostId
        AND p.isDeleted = false
        AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.id DESC
    """)
    List<UUID> findRepliesIds(
            @Param("parentPostId") UUID parentPostId,
            @Param("cursor") UUID cursor,
            Limit limit);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = GREATEST(p.commentCount - 1, 0) WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = p.reactionCount + 1 WHERE p.id = :postId")
    void incrementReactionCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = GREATEST(p.reactionCount - 1, 0) WHERE p.id = :postId")
    void decrementReactionCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.bookmarkCount = p.bookmarkCount + 1 WHERE p.id = :postId")
    void incrementBookmarkCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.bookmarkCount = GREATEST(p.bookmarkCount - 1, 0) WHERE p.id = :postId")
    void decrementBookmarkCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :postId")
    void incrementRepostCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = GREATEST(p.repostCount - 1, 0) WHERE p.id = :postId")
    void decrementRepostCount(@Param("postId") UUID postId);

    @Modifying
    @Query("""
        UPDATE Post p SET p.reactionCount = (
            SELECT COUNT(r) FROM Reaction r WHERE r.post = p
        )
    """)
    void syncAllReactionCounts();

    @Modifying
    @Query("""
        UPDATE Post p SET p.commentCount = (
            SELECT COUNT(c) FROM Post c WHERE c.parentPost = p AND c.isDeleted = false
        )
    """)
    void syncAllCommentCounts();

    @Modifying
    @Query("""
        UPDATE Post p SET p.repostCount = (
            SELECT COUNT(r) FROM Repost r WHERE r.post = p
        )
    """)
    void syncAllRepostCounts();

    @Modifying
    @Query("""
        UPDATE Post p SET p.bookmarkCount = (
            SELECT COUNT(b) FROM Bookmark b WHERE b.post = p
        )
    """)
    void syncAllBookmarkCounts();

    @Modifying
    @Query("""
        UPDATE Post p SET p.viewCount = (
            SELECT COUNT(v) FROM PostView v WHERE v.post = p AND v.user IS NOT NULL
        )
    """)
    void syncAllViewCounts();

    // Tìm kiếm post theo keyword trong nội dung
    @Query("""
        SELECT p.id FROM Post p
        WHERE p.visibility = 'PUBLIC'
        AND p.isDeleted = false
        AND LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.id DESC
    """)
    List<UUID> searchIdsByKeyword(@Param("keyword") String keyword,
                                  @Param("cursor") UUID cursor,
                                  Limit limit);
    Optional<Post> findByIdAndUserId(UUID id, UUID userId);
    @Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.media
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.quotePost qp
    LEFT JOIN FETCH qp.media
    LEFT JOIN FETCH qp.user
    LEFT JOIN FETCH p.postHashtags ph
    LEFT JOIN FETCH ph.hashtag
    LEFT JOIN FETCH p.mentions m
    LEFT JOIN FETCH m.user
    WHERE p.id = :postId
    AND p.user.id = :userId
    AND p.isDeleted = false
""")
    Optional<Post> findByIdAndUserIdWithDetails(@Param("postId") UUID postId, @Param("userId") UUID userId);
    Optional<Post> findByIdAndIsDeletedFalse(UUID id);
}
