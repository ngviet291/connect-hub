package com.connecthub.entity;

import com.connecthub.enums.ConversationType;
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
public class Conversation extends BaseEntity{
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @Enumerated(EnumType.STRING)
    private ConversationType type;

    @OneToMany(mappedBy = "conversation")
    @ToString.Exclude
    private Set<ConversationMember> conversationMembers;

    @OneToMany(mappedBy = "conversation")
    @ToString.Exclude
    private Set<Message> messages;
}
