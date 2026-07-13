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
            SELECT c FROM Conversation c
            WHERE c.type = 'PRIVATE'
              AND EXISTS (SELECT 1 FROM ConversationMember m1 WHERE m1.conversation = c AND m1.user.id = :userAId)
              AND EXISTS (SELECT 1 FROM ConversationMember m2 WHERE m2.conversation = c AND m2.user.id = :userBId)
            """)
    Optional<Conversation> findPrivateConversation(
            @Param("userAId") UUID userAId,
            @Param("userBId") UUID userBId
    );

    @Query("""
            SELECT c.id FROM Conversation c
            WHERE c.type = 'PRIVATE'
              AND EXISTS (SELECT 1 FROM ConversationMember m1 WHERE m1.conversation = c AND m1.user.id = :userAId)
              AND EXISTS (SELECT 1 FROM ConversationMember m2 WHERE m2.conversation = c AND m2.user.id = :userBId)
            """)
    Optional<UUID> findPrivateConversationId(UUID userAId, UUID userBId);

    // ConversationRepository
    @Query("""
            SELECT c FROM Conversation c
            JOIN c.conversationMembers cm
            WHERE cm.user.id = :userId
              AND (:status IS NULL OR cm.status = :status)
              AND (:cursor IS NULL OR c.id < :cursor) 
              AND cm.status not in ('LEFT', 'REMOVED')
            ORDER BY c.id DESC
            """)
    List<Conversation> findConversationsByUserId(
            @Param("userId") UUID userId,
            @Param("cursor") UUID cursor,
            Limit limit,
            @Param("status") MemberStatus status
    );

    @Query("""
    SELECT c FROM Conversation c
    LEFT JOIN FETCH c.conversationMembers cm
    LEFT JOIN FETCH cm.user
    WHERE c.id = :id
    """)
    Optional<Conversation> findByIdWithMembers(@Param("id") UUID id);

}
