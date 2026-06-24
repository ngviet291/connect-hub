package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageNotificationEvent implements DomainEvent {
    private UUID recipientId;
    private MessageResponse message;
    private ConversationType conversationType;

}
