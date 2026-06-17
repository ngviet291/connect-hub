package com.connecthub.modules.features.chat.repository;

import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {
}
