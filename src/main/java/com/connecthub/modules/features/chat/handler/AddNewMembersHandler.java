package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.event.AddNewMembersEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddNewMembersHandler implements EventHandler<AddNewMembersEvent> {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public Class<AddNewMembersEvent> support() {
        return AddNewMembersEvent.class;
    }

    @Override
    public void handle(AddNewMembersEvent event) {
        simpMessagingTemplate.convertAndSend("/topic/conversation/" + event.getConversationId() + "/event", event);

        // gui thong bao cho moi thanh vien moi duoc them vao
        event.getNewMembers().forEach(member -> {
            simpMessagingTemplate.convertAndSendToUser(
                    member.getUserId().toString(),
                    "/queue/conversations",
                    event
            );
        });
    }
}
