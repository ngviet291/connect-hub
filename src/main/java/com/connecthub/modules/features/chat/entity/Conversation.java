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
@Table(name = "conversations")
public class Conversation extends BaseEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ConversationType type;
    // Chỉ có giá trị khi GROUP và admin tự đặt tên. Với PRIVATE luôn null —
    // không bao giờ lưu username vào đây.
    private String name;

    // Tương tự: avatar riêng của group do admin upload, không liên quan
    // avatar của bất kỳ thành viên nào.
    private String avatarUrl;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConversationMember> conversationMembers;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages;
}
