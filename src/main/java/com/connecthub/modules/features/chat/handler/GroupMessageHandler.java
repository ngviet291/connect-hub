package com.connecthub.modules.features.chat.handler;

import com.connecthub.common.websocket.handler.EventHandler;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.event.GroupMessageEvent;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.service.DeliveryTrackingService;
import com.connecthub.modules.features.notification.dto.response.NotificationUserSummaryResponse;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.event.NotificationEvent;
import com.connecthub.modules.features.notification.service.NotificationService;
import com.connecthub.modules.features.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupMessageHandler implements EventHandler<GroupMessageEvent> {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserMapper userMapper; // map User -> NotificationUserSummaryResponse cho actor
    private final DeliveryTrackingService deliveryTrackingService;

    @Override
    public Class<GroupMessageEvent> support() {
        return GroupMessageEvent.class;
    }

    @Override
    public void handle(GroupMessageEvent event) {
        MessageResponse message = event.getMessage();

        // Luôn gửi notification cho MỌI member khác (trừ sender), không
        // phân biệt ai đang mở room hay không — chấp nhận dư thừa nếu họ
        // đang mở sẵn, đơn giản và nhất quán với cách PRIVATE đang làm.
        List<UUID> recipientIds = conversationMemberRepository
                .findUserIdsByConversationIdExcluding(event.getConversationId(), message.getSenderId());

        NotificationUserSummaryResponse actor = userMapper.toNotificationUserSummaryResponse(message.getSenderId(), message.getSenderUsername(), message.getSenderAvatarUrl());

        recipientIds.forEach(recipientId -> notificationService.pushNotification(
                NotificationEvent.builder()
                        .recipientId(recipientId)
                        .type(NotificationType.MESSAGE)
                        .content(message.getSenderUsername() + ": " + message.getContent())
                        .actor(actor)
                        .entityId(event.getConversationId())
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        // Broadcast 1 lần duy nhất qua topic chung — không lặp theo member.
        String destination = "/topic/conversations/" + event.getConversationId() + "/messages";
        simpMessagingTemplate.convertAndSend(destination, event);

        deliveryTrackingService.markDelivered(message.getMessageId(), ConversationType.GROUP, null);
    }
}