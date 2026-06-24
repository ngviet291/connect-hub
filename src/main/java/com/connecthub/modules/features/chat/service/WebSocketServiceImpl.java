package com.connecthub.modules.features.chat.service;

import com.connecthub.common.service.WebSocketService;
import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public void pushGroupMessage(UUID conversationId, MessageResponse response) {
        applicationEventPublisher.publishEvent(GroupMessageEvent.builder()
                .conversationId(conversationId)
                .message(response)
                .build());
    }

    @Override
    public void pushMessageDeleted(UUID messageId, UUID conversationId) {
        applicationEventPublisher.publishEvent(
                MessageDeletedNotificationEvent.builder()
                        .messageId(messageId)
                        .conversationId(conversationId)
                        .build()
        );
    }

    @Override
    public void pushAddNewMembers(UUID conversationId, List<ConversationMemberResponse> newMembers) {
        applicationEventPublisher.publishEvent(
                AddNewMembersEvent.builder()
                        .conversationId(conversationId)
                        .newMembers(newMembers)
                        .build()
        );
    }

    @Override
    public void pushUpdateMemberRole(UUID conversationId, ConversationMemberResponse memberResponse) {
        applicationEventPublisher.publishEvent(
                UpdateMemberRoleEvent.builder()
                        .conversationId(conversationId)
                        .memberResponse(memberResponse)
                        .build()
        );
    }
}