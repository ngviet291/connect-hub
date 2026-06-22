package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversation = :conversation AND cm.user != :user")
    Optional<ConversationMember> findByConversationAndUserNot(
            @Param("conversation") Conversation conversation,
            @Param("user") User user);

    Optional<ConversationMember> findConversationMemberByConversationIdAndUserId(UUID conversationId, UUID userId);
}
