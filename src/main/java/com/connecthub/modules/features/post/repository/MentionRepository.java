package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.Mention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentionRepository extends JpaRepository<Mention, UUID> {
}
