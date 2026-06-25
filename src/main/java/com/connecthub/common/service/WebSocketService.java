package com.connecthub.common.service;

import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;

import java.util.List;
import java.util.UUID;

public interface WebSocketService {

    void pushMessage(UUID recipientId, MessageResponse message, ConversationType conversationType);

    void pushGroupMessage(UUID id, MessageResponse response);


    void pushMessageDeleted(UUID messageId, UUID conversationId);

    void pushAddNewMembers(UUID conversationId, List<ConversationMemberResponse> newMembers);

    void pushUpdateMemberRole(UUID conversationId, ConversationMemberResponse memberResponse);
}
