package com.connecthub.modules.features.notification.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    private String content;
    private String targetUrl;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    @Builder.Default
    private boolean isRead = false;

}
