package com.connecthub.modules.features.chat.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Conversation extends BaseEntity {
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @Enumerated(EnumType.STRING)
    private ConversationType type;

    @OneToMany(mappedBy = "conversation")
    private Set<ConversationMember> conversationMembers;

    @OneToMany(mappedBy = "conversation")
    private Set<Message> messages;
}
