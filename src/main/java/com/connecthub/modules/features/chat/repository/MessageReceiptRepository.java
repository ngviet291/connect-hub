package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {

    // MessageReceiptRepository
    @Query("""
            SELECT r.message.conversation.id, COUNT(r)
            FROM MessageReceipt r
            WHERE r.message.conversation.id IN :conversationIds
              AND r.user.id = :userId
              AND r.status <> :readStatus
            GROUP BY r.message.conversation.id
            """)
    List<Object[]> countUnreadGroupedByConversation(
            @Param("conversationIds") List<UUID> conversationIds,
            @Param("userId") UUID userId,
            @Param("readStatus") MessageStatus readStatus
    );
}
