package com.connecthub.modules.features.user.repository;

import com.connecthub.modules.features.user.entity.UserBlock;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    // hàm kiểm tra a có block b hay không
    boolean existsByBlockedIdAndBlockerId(UUID blockedId, UUID blockerId);
    void deleteByBlockedIdAndBlockerId(UUID blockedId, UUID blockerId);
    List<UserBlock> findByBlockerId(UUID blockerId);

    @Query("""
        SELECT ub
        FROM UserBlock ub
        LEFT JOIN FETCH ub.blocked
        WHERE ub.blocker.id = :blockerId
        AND (:cursor IS NULL OR ub.id < :cursor)
        ORDER BY ub.id DESC
    """)
    List<UserBlock> findBlockedUsers(UUID blockerId, UUID cursor, Limit limit);
}
