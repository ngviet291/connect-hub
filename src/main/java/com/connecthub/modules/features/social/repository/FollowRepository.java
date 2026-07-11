package com.connecthub.modules.features.social.repository;

import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.projection.FollowingRowProjection;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);


    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // Optimized queries with JOIN FETCH to avoid N+1 problem
    @Query("""
                SELECT DISTINCT f
                FROM Follow f
                LEFT JOIN FETCH f.follower
                WHERE f.following.id = :userId
                AND (:cursor IS NULL OR f.id < :cursor)
                ORDER BY f.id DESC
            """)
    List<Follow> findFollowersOptimized(UUID userId, UUID cursor, Limit limit);

    @Query("""
                SELECT DISTINCT f
                FROM Follow f
                LEFT JOIN FETCH f.following
                WHERE f.follower.id = :userId
                AND (:cursor IS NULL OR f.id < :cursor)
                ORDER BY f.id DESC
            """)
    List<Follow> findFollowingOptimized(UUID userId, UUID cursor, Limit limit);

    @Query("""
            SELECT f.id AS followId,
                   u.id AS id,
                   u.username AS username,
                   u.fullName AS fullName,
                   u.avatarUrl AS avatarUrl,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM Follow vf
                       WHERE vf.follower.id = :viewerId
                       AND vf.following.id = u.id
                   ) THEN true ELSE false END AS isFollowing
            FROM Follow f
            JOIN f.following u
            WHERE f.follower.id = :userId
            AND (:cursor IS NULL OR f.id < :cursor)
            ORDER BY f.id DESC
            """)
    List<FollowingRowProjection> findFollowers(UUID userId, UUID viewerId, UUID cursor, Limit limit);

    @Query("""
        SELECT f.id AS followId,
               u.id AS id,
               u.username AS username,
               u.fullName AS fullName,
               u.avatarUrl AS avatarUrl,
               CASE WHEN EXISTS (
                   SELECT 1 FROM Follow vf
                   WHERE vf.follower.id = :viewerId
                   AND vf.following.id = u.id
               ) THEN true ELSE false END AS isFollowing
        FROM Follow f
        JOIN f.follower u
        WHERE f.following.id = :userId
        AND (:cursor IS NULL OR f.id < :cursor)
        ORDER BY f.id DESC
        """)
    List<FollowingRowProjection> findFollowing(UUID userId, UUID viewerId, UUID cursor, Limit limit);}
