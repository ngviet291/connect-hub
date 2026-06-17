package com.connecthub.modules.features.social.repository;

import com.connecthub.modules.features.social.entity.FeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, UUID> {
}
