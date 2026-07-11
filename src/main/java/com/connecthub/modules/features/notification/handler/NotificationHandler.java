package com.connecthub.modules.features.notification.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.notification.dto.request.NotificationRequest;
import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.event.NotificationEvent;
import com.connecthub.modules.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler implements EventHandler<NotificationEvent> {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;

    @Override
    public Class<NotificationEvent> support() {
        return NotificationEvent.class;
    }

    @Override
    public void handle(NotificationEvent event) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .recipient(event.getRecipientId())
                .actor(event.getActor().getId())
                .content(event.getContent())
                .type(NotificationType.FOLLOW)
                .build();

       NotificationResponse notificationResponse = notificationService.createNotification(notificationRequest);
        log.info("Notification sent to user {}: {}", event.getRecipientId(), event.getContent());
        simpMessagingTemplate.convertAndSendToUser(
                event.getRecipientId().toString(),
                "/queue/notifications",
                notificationResponse
        );
    }
}
