package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {
}
