package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, UUID> {
}