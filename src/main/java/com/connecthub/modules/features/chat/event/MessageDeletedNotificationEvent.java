package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MessageDeletedNotificationEvent implements DomainEvent {
    private UUID messageId;
    private UUID conversationId;

}
