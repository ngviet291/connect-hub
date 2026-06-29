package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.dto.projection.ReactionTypeCountProjection;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    Optional<Reaction> findByPostIdAndUserId(UUID postId, UUID userId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    @Query("""
        SELECT r FROM Reaction r
        JOIN FETCH r.user
        WHERE r.post.id = :postId
        AND (:cursor IS NULL OR r.id < :cursor)
        ORDER BY r.id DESC
    """)
    List<Reaction> findByPostIdWithUser(
            @Param("postId") UUID postId,
            @Param("cursor") UUID cursor,
            Pageable pageable);

    @Query("""
        SELECT r.type AS type, COUNT(r) AS count
        FROM Reaction r
        WHERE r.post.id = :postId
        GROUP BY r.type
    """)
    List<ReactionTypeCountProjection> countByPostIdGroupByType(@Param("postId") UUID postId);
}