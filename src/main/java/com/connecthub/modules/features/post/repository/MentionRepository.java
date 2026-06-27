package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Mention;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentionRepository extends JpaRepository<Mention, UUID> {

    // Lấy danh sách người được mention trong một bài đăng
    @Query("""
        SELECT m FROM Mention m
        JOIN FETCH m.user
        WHERE m.post.id = :postId
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
    """)
    List<Mention> findByPostId(@Param("postId") UUID postId,
                               @Param("cursor") UUID cursor,
                               Limit limit);

    // Lấy bài đăng đang mention người dùng hiện tại
    @Query("""
        SELECT m FROM Mention m
        JOIN FETCH m.post p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.media
        LEFT JOIN FETCH p.postHashtags ph
        LEFT JOIN FETCH ph.hashtag
        LEFT JOIN FETCH p.mentions mm
        LEFT JOIN FETCH mm.user
        WHERE m.user.id = :userId
        AND p.isDeleted = false
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
    """)
    List<Mention> findByUserId(@Param("userId") UUID userId,
                               @Param("cursor") UUID cursor,
                               Limit limit);
    @Modifying
    @Query("DELETE FROM Mention m WHERE m.post.id = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}
