package com.connecthub.modules.features.chat.service;

import com.connecthub.common.service.WebSocketService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.exception.InvalidChatRequestException;
import com.connecthub.modules.features.chat.exception.InvalidTypeConversionException;
import com.connecthub.modules.features.chat.exception.SenderNotConversationMemberException;
import com.connecthub.modules.features.chat.mapper.MessageMapper;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.social.service.FollowService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
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

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public MessageResponse sendFirstMessage(SendMessageRequest request) {

        ObjectMapper objectMapper = new ObjectMapper();

        if (request.getContent() == null && (request.getMedia() == null || request.getMedia().isEmpty())) {
            throw new InvalidChatRequestException();
        }

        UUID senderId = AppUtil.userIdFormAuthentication();

        if (!userRepository.existsById(request.getRecipientId())) {
            throw new UserNotFoundException();
        }

        //TODO: nếu đang bị block thì không cho gửi tin nhắn ném lỗi, đợi userService làm xong

        Optional<Conversation> existing = conversationRepository
                .findPrivateConversation(senderId, request.getRecipientId());
        // BLOCK xử lí ở todo


        // tao proxy entity để tránh truy vấn thừa
        User recipient = userRepository.getReferenceById(request.getRecipientId());
        User sender = userRepository.getReferenceById(senderId);
        if (existing.isPresent()) {
            return sendToExistingConversation(existing.get(), sender, request);
        }

        MemberStatus recipientStatus = followService.isMutualFollow(senderId, request.getRecipientId())
                ? MemberStatus.ACCEPTED
                : MemberStatus.PENDING;

        System.out.println("recipientStatus: " + recipientStatus);
        // tạo conversion mới
        Conversation conversation = conversationService.createPrivateConversation(
                sender, recipient, recipientStatus
        );


        Message message = messageService.saveMessage(conversation, sender, request);

        MessageResponse messageResponse = messageMapper.toResponse(
                message,
                message.getSender(),
                conversation,
                recipientStatus,
                MessageStatus.SENT
        );

        if (recipientStatus == MemberStatus.ACCEPTED) {
            webSocketService.pushMessage(request.getRecipientId(), messageResponse);
        } else {
            webSocketService.pushPendingNotification(request.getRecipientId(), conversation);
        }
        // push notification to recipient by WS
        return messageResponse;
    }


    private MessageResponse sendToExistingConversation(Conversation conversation, User sender, SendMessageRequest request) {
        if (conversation.getType() != ConversationType.PRIVATE) {
            throw new InvalidTypeConversionException(ConversationType.PRIVATE);
        }
        // tao proxy entity để tránh truy vấn thừa
        Message message = messageService.saveMessage(conversation, sender, request);
        // Lấy status thực tế của recipient từ DB
        MemberStatus recipientStatus = conversationMemberRepository
                .findByConversationAndUserNot(conversation, sender) // lấy member không phải sender
                .orElseThrow(SenderNotConversationMemberException::new)
                .getStatus();

        // push notification to recipient by WS
        MessageResponse messageResponse = messageMapper.toResponse(
                message,
                message.getSender(),
                conversation,
                recipientStatus,
                MessageStatus.SENT
        );
        if (recipientStatus == MemberStatus.ACCEPTED) {
            webSocketService.pushMessage(request.getRecipientId(), messageResponse);
        } else {
            webSocketService.pushPendingNotification(request.getRecipientId(), conversation);
        }
        return messageResponse;
    }
}
