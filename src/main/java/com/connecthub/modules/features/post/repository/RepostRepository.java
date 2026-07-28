package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.dto.projection.RepostedPostIdProjection;
import com.connecthub.modules.features.post.entity.Repost;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RepostRepository extends JpaRepository<Repost, UUID> {
    Optional<Repost> findByPostIdAndUserId(UUID postId, UUID userId);
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    @Query("SELECT r.post.id FROM Repost r WHERE r.user.id = :userId AND r.post.id IN :postIds")
    Set<UUID> findRepostedPostIds(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
    @Query("""
        SELECT r.id AS repostId, r.post.id AS postId FROM Repost r
        WHERE r.user.username = :username
        AND r.post.isDeleted = false
        AND (
            r.post.visibility = 'PUBLIC'
            OR (:currentUserId IS NOT NULL AND r.post.user.id = :currentUserId)
        )
        AND (:cursor IS NULL OR r.id < :cursor)
        ORDER BY r.id DESC
    """)
    List<RepostedPostIdProjection> findUserRepostedPostIds(@Param("username") String username,
                                                           @Param("currentUserId") UUID currentUserId,
                                                           @Param("cursor") UUID cursor,
                                                           Limit limit);
}