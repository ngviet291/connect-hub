package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.MessageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageMediaRepository extends JpaRepository<MessageMedia, UUID> {
}
