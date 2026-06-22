package com.connecthub.modules.features.chat.service;

import com.connecthub.common.service.WebSocketService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.exception.*;
import com.connecthub.modules.features.chat.mapper.MessageMapper;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.notification.event.NotificationEvent;
import com.connecthub.modules.features.notification.service.NotificationService;
import com.connecthub.modules.features.social.service.FollowService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.connecthub.modules.features.user.service.UserBlockService;
import com.connecthub.modules.features.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final FollowService followService;
    private final MessageMapper messageMapper;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final WebSocketService webSocketService;
    private final UserBlockService userBlockService;
    private final NotificationService notificationService;

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public MessageResponse sendMessage(SendMessageRequest request) {
        validateContentPresent(request);

        UUID senderId = AppUtil.userIdFormAuthentication();
        UUID recipientId = request.getRecipientId();

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(UserNotFoundException::new);
        validateNotBlocked(senderId, recipientId);

        User sender = userRepository.getReferenceById(senderId);

        Optional<Conversation> existing = conversationRepository.findPrivateConversation(senderId, recipientId);
        if (existing.isPresent()) {
            return sendToExistingConversation(existing.get(), sender, request);
        }

        // tạo conversation mới nếu chưa có, và lưu message vào conversation đó
        MemberStatus recipientStatus = followService.isMutualFollow(senderId, recipientId)
                ? MemberStatus.ACCEPTED
                : MemberStatus.PENDING;

        Conversation conversation = conversationService.createPrivateConversation(sender, recipient, recipientStatus);
        Message message = messageService.saveMessage(conversation, sender, request);

        MessageResponse response = messageMapper.toResponse(
                message, message.getSender(), conversation, recipientStatus, MessageStatus.SENT
        );

        pushToRecipient(recipientId, recipientStatus, response);
        return response;
    }

    private MessageResponse sendToExistingConversation(Conversation conversation, User sender, SendMessageRequest request) {
        if (conversation.getType() != ConversationType.PRIVATE) {
            throw new InvalidTypeConversionException(ConversationType.PRIVATE);
        }

        Message message = messageService.saveMessage(conversation, sender, request);

        MemberStatus recipientStatus = resolveAndMaybePromoteRecipientStatus(
                conversation, sender, request.getRecipientId()
        );

        MessageResponse response = messageMapper.toResponse(
                message, message.getSender(), conversation, recipientStatus, MessageStatus.SENT
        );

        pushToRecipient(request.getRecipientId(), recipientStatus, response);
        return response;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void validateContentPresent(SendMessageRequest request) {
        boolean noContent = request.getContent() == null;
        boolean noMedia = request.getMedia() == null || request.getMedia().isEmpty();
        if (noContent && noMedia) {
            throw new InvalidChatRequestException();
        }
    }

    private void validateNotBlocked(UUID senderId, UUID recipientId) {
        if (userBlockService.isBlockedBy(senderId, recipientId)) {
            throw new BlockedBySenderException();
        }
        if (userBlockService.isBlockedBy(recipientId, senderId)) {
            throw new RecipientBlockedException();
        }
    }

    // Đọc status đã lưu; chỉ nâng PENDING → ACCEPTED nếu giờ đã mutual follow.
    // Không bao giờ hạ ACCEPTED → PENDING (vd sau khi unfollow) — một khi đã
    // accept thì giữ nguyên.
    private MemberStatus resolveAndMaybePromoteRecipientStatus(
            Conversation conversation, User sender, UUID recipientId
    ) {
        ConversationMember recipientMember = conversationMemberRepository
                .findByConversationAndUserNot(conversation, sender)
                .orElseThrow(SenderNotConversationMemberException::new);

        MemberStatus status = recipientMember.getStatus();
        if (status == MemberStatus.PENDING && followService.isMutualFollow(sender.getId(), recipientId)) {
            status = MemberStatus.ACCEPTED;
            recipientMember.setStatus(status);
            conversationMemberRepository.save(recipientMember);
        }
        return status;
    }

    private void pushToRecipient(UUID recipientId, MemberStatus status, MessageResponse response) {
        if (status == MemberStatus.ACCEPTED) {
            webSocketService.pushMessage(recipientId, response);
        } else {
            notificationService.pushNotification(
                    NotificationEvent.builder()
                            .recipientId(recipientId)
                            .content(response.getContent())
                            .actor(com.connecthub.modules.features.notification.dto.response.UserSummaryResponse.builder()
                                    .id(response.getSenderId())
                                    .username(response.getSenderUsername())
                                    .avatarUrl(response.getSenderAvatarUrl())
                                    .build())
                            .entityId(response.getConversationId())
                            .type(com.connecthub.modules.features.notification.enums.NotificationType.MESSAGE)
                            .createdAt(java.time.LocalDateTime.now())
                            .build()
            );
        }
    }
}
