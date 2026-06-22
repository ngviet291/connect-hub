package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
                SELECT DISTINCT c FROM Conversation c
                JOIN c.conversationMembers cm1
                JOIN c.conversationMembers cm2
                WHERE c.type = 'PRIVATE'
                  AND cm1.user.id = :senderId
                  AND cm2.user.id = :recipientId
            """)
    Optional<Conversation> findPrivateConversation(
            @Param("senderId") UUID senderId,
            @Param("recipientId") UUID recipientId
    );

    // ConversationRepository
    @Query("""
    SELECT c FROM Conversation c
    JOIN c.conversationMembers cm
    WHERE cm.user.id = :userId
      AND (:status IS NULL OR cm.status = :status)
      AND (:cursor IS NULL OR c.id < :cursor)
    ORDER BY c.id DESC
    """)
    List<Conversation> findConversationsByUserId(
            @Param("userId") UUID userId,
            @Param("cursor") UUID cursor,
            Limit limit,
            @Param("status") MemberStatus status
    );
}
