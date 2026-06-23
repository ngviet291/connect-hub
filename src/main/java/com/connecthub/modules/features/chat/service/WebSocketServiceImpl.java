package com.connecthub.modules.features.chat.service;

import com.connecthub.common.service.WebSocketService;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.event.MessageNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void pushMessage(UUID recipientId, MessageResponse message, ConversationType conversationType) {
        applicationEventPublisher.publishEvent(MessageNotificationEvent.builder()
                .recipientId(recipientId)
                .message(message)
                .conversationType(conversationType)
                .build());
    }


}