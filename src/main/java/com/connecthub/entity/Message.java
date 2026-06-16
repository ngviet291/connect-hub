package com.connecthub.entity;

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
public class Message extends BaseEntity {
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User sender;
    private String content;

    @OneToMany(mappedBy = "message")
    @ToString.Exclude
    private Set<MessageReceipt> messageReceipts;
    @OneToMany(mappedBy = "message")
    @ToString.Exclude
    private Set<MessageMedia> messageMedia;
}
