package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
    long countByPostId(UUID postId);

    List<Media> findByPostId(UUID postId);
}
