package com.connecthub.modules.features.chat.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GroupMessageEvent implements DomainEvent {
    private UUID conversationId;
    private MessageResponse message;
}