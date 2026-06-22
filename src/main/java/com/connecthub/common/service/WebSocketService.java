package com.connecthub.common.service;

import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;

import java.util.UUID;

public interface WebSocketService {
    void pushMessage(UUID recipientId, MessageResponse message);

    void pushPendingNotification(UUID recipientId, MessageResponse messageResponse);
}
