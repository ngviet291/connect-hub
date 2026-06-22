package com.connecthub.modules.features.user.repository;

import com.connecthub.modules.features.user.dto.response.FollowStatsResponse;
import com.connecthub.modules.features.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

     boolean existsByUsernameAndIdNot(String username, UUID id);

     @Query("SELECT u FROM User u WHERE u.id = :id AND u.isActive = true")
     Optional<User> findActiveById(UUID id);
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
}
