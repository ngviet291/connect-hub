package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.event.UpdateMemberRoleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateMemberRoleHandler implements EventHandler<UpdateMemberRoleEvent> {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public Class<UpdateMemberRoleEvent> support() {
        return UpdateMemberRoleEvent.class;
    }

    @Override
    public void handle(UpdateMemberRoleEvent event) {
        simpMessagingTemplate.convertAndSend("/topic/conversation/" + event.getConversationId() + "/event", event);
    }
}
