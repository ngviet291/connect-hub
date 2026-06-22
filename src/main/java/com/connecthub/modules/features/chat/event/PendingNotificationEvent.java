package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PendingNotificationEvent implements DomainEvent {
    private UUID senderId;
    private String senderUsername;
    private String senderAvatarUrl;
    private String firstMessagePreview;
    private MessageResponse messageResponse;
    private UUID recipientId;
}