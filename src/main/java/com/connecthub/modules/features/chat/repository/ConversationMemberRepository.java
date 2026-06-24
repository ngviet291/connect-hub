package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.user.entity.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.*;


@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversation = :conversation AND cm.user != :user")
    Optional<ConversationMember> findByConversationAndUserNot(
            @Param("conversation") Conversation conversation,
            @Param("user") User user);

    Optional<ConversationMember> findConversationMemberByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("""
    SELECT cm.user.id FROM ConversationMember cm
    WHERE cm.conversation.id = :conversationId AND cm.user.id <> :excludingUserId
    """)
    List<UUID> findUserIdsByConversationIdExcluding(
            @Param("conversationId") UUID conversationId,
            @Param("excludingUserId") UUID excludingUserId
    );
    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("SELECT cm.user.id FROM ConversationMember cm WHERE cm.conversation.id = :conversationId")
    Set<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);

    long countByConversationId(UUID conversationId);

    List<ConversationMember> findAllByConversationIdAndUserIdIn(UUID conversationId, Collection<UUID> userIds);

    long countByConversationIdAndStatusIn(UUID conversationId, Collection<MemberStatus> statuses);


    @Modifying
    @Query(value = """
    INSERT INTO conversation_member (conversation_id, user_id, role, status, joined_at, left_at)
    VALUES (:conversationId, :userId, 'MEMBER', 'ACCEPTED', :now, NULL)
    ON CONFLICT (conversation_id, user_id)
    DO UPDATE SET
        status = 'ACCEPTED',
        role = 'MEMBER',
        joined_at = :now,
        left_at = NULL
    WHERE conversation_member.status NOT IN ('ACCEPTED', 'PENDING')
    """, nativeQuery = true)
    int upsertMember(@Param("conversationId") UUID conversationId,
                     @Param("userId") UUID userId,
                     @Param("now") LocalDateTime now);

    // ConversationMemberRepository
    @Query(value = """
    SELECT cm FROM ConversationMember cm
    JOIN FETCH cm.user
    WHERE cm.conversation.id = :conversationId
      AND (:cursor IS NULL OR cm.user.id > :cursor) AND cm.status NOT IN ('LEFT', 'REMOVED')
    ORDER BY cm.user.id ASC
    """)
    List<ConversationMember> findMembersByConversationId(
            @Param("conversationId") UUID conversationId,
            @Param("cursor") UUID cursor,
            @Param("limit") Limit limit);

    @Query("""
    SELECT cm FROM ConversationMember cm
    JOIN FETCH cm.user
    WHERE cm.conversation.id = :conversationId
      AND cm.user.id <> :userId
    """)
    Optional<ConversationMember> findConversationMemberByConversationIdAndUserIdNot(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);
}
