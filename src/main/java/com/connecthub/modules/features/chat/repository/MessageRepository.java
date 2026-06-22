package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query(value = """
            SELECT DISTINCT ON (conversation_id) *
            FROM messages
            WHERE conversation_id IN :conversationIds
            ORDER BY conversation_id, id DESC
            """, nativeQuery = true)
    List<Message> findLastMessagesForConversations(@Param("conversationIds") List<UUID> conversationIds);
}
