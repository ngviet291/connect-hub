package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Bookmark;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByPostIdAndUserId(UUID postId, UUID userId);

    @Query("""
        SELECT b FROM Bookmark b
        LEFT JOIN FETCH b.post p
        LEFT JOIN FETCH p.media
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.quotePost qp
        LEFT JOIN FETCH qp.media
        LEFT JOIN FETCH qp.user
        WHERE b.user.id = :userId
        AND (:cursor IS NULL OR b.id < :cursor)
        ORDER BY b.id DESC
    """)
    List<Bookmark> findByUserIdWithDetails(
            @Param("userId") UUID userId,
            @Param("cursor") UUID cursor,
            Limit limit);
}