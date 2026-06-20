package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Repost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepostRepository extends JpaRepository<Repost, UUID> {
    Optional<Repost> findByPostIdAndUserId(UUID postId, UUID userId);
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
}