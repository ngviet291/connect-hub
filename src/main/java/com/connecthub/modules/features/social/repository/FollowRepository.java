package com.connecthub.modules.features.social.repository;

import com.connecthub.modules.features.social.entity.Follow;
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

    long countByFollowingId(UUID followingId);

    long countByFollowerId(UUID followerId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("""
			SELECT f
			FROM Follow f
			WHERE f.following.id = :userId
			AND (:cursor IS NULL OR f.id < :cursor)
			ORDER BY f.id DESC
	""")
    List<Follow> findFollowers(UUID userId, UUID cursor, Limit limit);

    @Query("""
			SELECT f
			FROM Follow f
			WHERE f.follower.id = :userId
			AND (:cursor IS NULL OR f.id < :cursor)
			ORDER BY f.id DESC
	""")
    List<Follow> findFollowing(UUID userId, UUID cursor, Limit limit);

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
    List<Follow> findFollowingOptimized(UUID userId, UUID cursor, Limit limit);}
