package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// PendingNotificationEvent.java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PendingNotificationEvent implements DomainEvent {
    private UUID senderId;
    private UUID recipientId;
    private UUID conversationId;
    private String senderUsername;
    private String senderAvatarUrl;
    private String firstMessagePreview;
}