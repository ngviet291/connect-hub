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
    import com.connecthub.modules.features.chat.repository.MessageRepository;
    import com.connecthub.modules.features.notification.dto.response.NotificationUserSummaryResponse;
    import com.connecthub.modules.features.notification.event.NotificationEvent;
    import com.connecthub.modules.features.notification.service.NotificationService;
    import com.connecthub.modules.features.social.service.FollowService;
    import com.connecthub.modules.features.user.entity.User;
    import com.connecthub.modules.features.user.exception.UserNotFoundException;
    import com.connecthub.modules.features.user.repository.UserRepository;
    import com.connecthub.modules.features.user.service.UserBlockService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.Optional;
    import java.util.UUID;

    @Slf4j
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
        private final MessageRepository messageRepository;

        @Transactional
        @PreAuthorize("hasRole('USER')")
        public MessageResponse sendMessage(SendMessageRequest request) {
            validateContentPresent(request);
            UUID senderId = AppUtil.userIdFormAuthentication();

            if (request.getConversationId() != null) {
                // Có conversationId → có thể là GROUP, hoặc PRIVATE gửi tiếp vào
                // conversation đã tồn tại. Load lên để biết chắc type, không đoán.
                Conversation conversation = conversationRepository.findById(request.getConversationId())
                        .orElseThrow(ConversationNotFoundException::new);

                if (conversation.getType() == ConversationType.GROUP) {
                    return sendGroupMessage(conversation, senderId, request);
                }
                // PRIVATE nhưng đã có conversationId — vẫn cần recipientId để biết
                // push WS cho ai (giữ đúng thiết kế cũ, không đổi).
                User sender = userRepository.getReferenceById(senderId);
                return sendToExistingConversation(conversation, sender, request);
            }

            // Không có conversationId → chỉ có thể là PRIVATE, tin đầu tiên.
            return sendFirstPrivateMessage(senderId, request);
        }

        private MessageResponse sendFirstPrivateMessage(UUID senderId, SendMessageRequest request) {
            UUID recipientId = request.getRecipientId();
            if (recipientId == null) {
                throw new RecipientNotProvidedException(); // PRIVATE bắt buộc phải có recipientId
            }

            User recipient = userRepository.findById(recipientId).orElseThrow(UserNotFoundException::new);
            validateNotBlocked(senderId, recipientId);
            User sender = userRepository.getReferenceById(senderId);

            Optional<Conversation> existing = conversationRepository.findPrivateConversation(senderId, recipientId);
            if (existing.isPresent()) {
                return sendToExistingConversation(existing.get(), sender, request);
            }

            MemberStatus recipientStatus = followService.isMutualFollow(senderId, recipientId)
                    ? MemberStatus.ACCEPTED : MemberStatus.PENDING;

            Conversation conversation = conversationService.createPrivateConversation(sender, recipient, recipientStatus);
            Message message = messageService.saveMessage(conversation, sender, request);

            MessageResponse response = messageMapper.toResponse(
                    message, message.getSender(), conversation, recipientStatus, MessageStatus.SENT);


            pushToRecipient(conversation, recipientId, recipientStatus, response);
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
            pushToRecipient(conversation, request.getRecipientId(), recipientStatus, response);
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


        @Transactional
        public void markConversationAsRead(UUID conversationId, UUID lastMessageId) {
            UUID currentUserId = AppUtil.userIdFormAuthentication();

            ConversationMember member = conversationMemberRepository
                    .findByConversationIdAndUserId(conversationId, currentUserId)
                    .orElseThrow(SenderNotConversationMemberException::new);

            Message lastMessage = messageRepository.findById(lastMessageId)
                    .orElseThrow(() -> new MessageNotFoundException(lastMessageId.toString()));

            // Chỉ cập nhật nếu lastMessageId mới hơn con trỏ hiện tại — tránh
            // 1 request cũ (vd do retry/race condition) kéo lùi trạng thái đọc.
            if (member.getLastReadMessage() == null
                    || lastMessage.getId().compareTo(member.getLastReadMessage().getId()) > 0) {
                member.setLastReadMessage(lastMessage);
                conversationMemberRepository.save(member);
            }
        }

        private void pushToRecipient(Conversation conversation, UUID recipientId, MemberStatus status, MessageResponse response) {

            if (status == MemberStatus.ACCEPTED) {
                webSocketService.pushMessage(recipientId, response, conversation.getType());
            } else {
                notificationService.pushNotification(
                        NotificationEvent.builder()
                                .recipientId(recipientId)
                                .content(response.getContent())
                                .actor(NotificationUserSummaryResponse.builder()
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


        private MessageResponse sendGroupMessage(Conversation conversation, UUID senderId, SendMessageRequest request) {
            ConversationMember senderMember = conversationMemberRepository
                    .findByConversationIdAndUserId(conversation.getId(), senderId)
                    .orElseThrow(ConversationAccessDeniedException::new);

            if (senderMember.getStatus() != MemberStatus.ACCEPTED) {
                throw new ConversationAccessDeniedException();
            }

            User sender = userRepository.getReferenceById(senderId);
            Message message = messageService.saveMessage(conversation, sender, request);

            MessageResponse response = messageMapper.toResponse(
                    message, message.getSender(), conversation, MemberStatus.ACCEPTED, MessageStatus.SENT);

            webSocketService.pushGroupMessage(conversation.getId(), response);

            return response;
        }
    }
