package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.event.PendingNotificationEvent;
import com.connecthub.modules.features.notification.dto.request.NotificationRequest;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PendingNotificationHandler implements EventHandler<PendingNotificationEvent> {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;

    public PendingNotificationHandler(SimpMessagingTemplate simpMessagingTemplate, NotificationService notificationService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationService = notificationService;
    }

    @Override
    public Class<PendingNotificationEvent> support() {
        return PendingNotificationEvent.class;
    }

    @Override
    public void handle(PendingNotificationEvent event) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .actor(event.getSenderId())
                .type(NotificationType.MESSAGE_PENDING)
                .recipient(event.getRecipientId())
                .conversationId(event.getConversationId())
                .build();

        notificationService.createNotification(notificationRequest);

        simpMessagingTemplate.convertAndSendToUser(
                event.getRecipientId().toString(),
                "/queue/pending",
                event
        );
    }
}
