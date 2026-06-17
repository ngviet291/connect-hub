package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {
}
