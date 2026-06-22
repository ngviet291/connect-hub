package com.connecthub.modules.features.chat.service;

import com.connecthub.common.service.WebSocketService;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.event.MessageNotificationEvent;
import com.connecthub.modules.features.chat.event.PendingNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void pushMessage(UUID recipientId, MessageResponse message) {
        applicationEventPublisher.publishEvent(MessageNotificationEvent.builder()
                .recipientId(recipientId)
                .message(message)
                .build());
    }

    // WebSocketServiceImpl.java
    @Override
    public void pushPendingNotification(UUID recipientId, MessageResponse messageResponse) {
        applicationEventPublisher.publishEvent(
                PendingNotificationEvent.builder()
                        .recipientId(recipientId)
                        .messageResponse(messageResponse)
                        .senderUsername(messageResponse.getSenderUsername())
                        .senderAvatarUrl(messageResponse.getSenderAvatarUrl())
                        .firstMessagePreview(messageResponse.getContent())
                        .senderId(messageResponse.getSenderId())
                        .build()
        );
    }
}