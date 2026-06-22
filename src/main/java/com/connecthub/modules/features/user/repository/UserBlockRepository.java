package com.connecthub.modules.features.user.repository;

import com.connecthub.modules.features.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    boolean existsByBlockedIdAndBlockerId(UUID blockedId, UUID blockerId);
}
