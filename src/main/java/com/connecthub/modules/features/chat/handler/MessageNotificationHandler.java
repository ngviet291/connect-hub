package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.event.MessageNotificationEvent;
import com.connecthub.modules.features.notification.dto.request.NotificationRequest;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageNotificationHandler implements EventHandler<MessageNotificationEvent> {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;

    @Override
    public Class<MessageNotificationEvent> support() {
        return MessageNotificationEvent.class;
    }

    @Override
    public void handle(MessageNotificationEvent event) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .actor(event.getMessage().getSenderId())
                .type(NotificationType.MESSAGE)
                .recipient(event.getRecipientId())
                .conversationId(event.getMessage().getConversationId())
                .build();

        notificationService.createNotification(notificationRequest);
        // 1) Broadcast cho ai ĐÃ subscribe đúng topic của conversation này
        //    (đang mở chat realtime trong room đó).
        String destination = "/topic/conversations/" + event.getMessage().getConversationId() + "/messages";
        simpMessagingTemplate.convertAndSend(destination, event);

        simpMessagingTemplate.convertAndSendToUser(
                event.getRecipientId().toString(),
                "/queue/messages",
                event
        );
    }


}
