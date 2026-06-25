package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.event.MessageDeletedNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDeletedNotificationHandler implements EventHandler<MessageDeletedNotificationEvent> {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public Class<MessageDeletedNotificationEvent> support() {
        return MessageDeletedNotificationEvent.class;
    }

    @Override
    public void handle(MessageDeletedNotificationEvent event) {
        simpMessagingTemplate.convertAndSend("/topic/conversations/" + event.getConversationId() + "/event", event);
    }
}
