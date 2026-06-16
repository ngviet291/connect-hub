package com.connecthub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(ConversationMemberId.class)
public class ConversationMember extends BaseEntity{
    @Id
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private LocalDateTime joinedAt;

}
