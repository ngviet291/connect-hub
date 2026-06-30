package com.connecthub.modules.features.user.repository;

import com.connecthub.modules.features.user.dto.response.FollowStatsResponse;
import com.connecthub.modules.features.user.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
                SELECT u FROM User u
                JOIN FETCH u.roles
                WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
            """)
    Optional<User> findByUsername(@Param("username") String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    @Query("""
                SELECT new com.connecthub.modules.features.user.dto.response.FollowStatsResponse(
                    COUNT(DISTINCT follower.id),
                    COUNT(DISTINCT following.id)
                )
                FROM User u
                LEFT JOIN u.followers follower
                LEFT JOIN u.following following
                WHERE u.id = :userId
                GROUP BY u.id
            """)
    FollowStatsResponse countFollowStats(UUID userId);

    @Query("SELECT u FROM User u WHERE :cursor IS NULL OR u.id < :cursor ORDER BY u.id DESC ")
    List<User> findAllUsers(@Param("cursor") UUID cursor, Limit limit);

    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND (
            LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:cursor IS NULL OR u.id < :cursor)
        ORDER BY u.id DESC
    """)
    List<User> searchByNameOrUsername(@Param("keyword") String keyword,
                                      @Param("cursor") UUID cursor,
                                      Limit limit);

    // Tìm chính xác theo username để dùng cho mention
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) = LOWER(TRIM(:username))
    """)
    Optional<User> findExactByUsername(@Param("username") String username);
    @Query("SELECT u FROM User u WHERE LOWER(u.username) IN :usernames")
    List<User> findAllByUsernameIn(@Param("usernames") Collection<String> usernames);
}
