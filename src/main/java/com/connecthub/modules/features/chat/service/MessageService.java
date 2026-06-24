package com.connecthub.modules.features.chat.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.service.WebSocketService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.SendMessageRequest;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageMedia;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.exception.ConversationAccessDeniedException;
import com.connecthub.modules.features.chat.exception.ConversationNotFoundException;
import com.connecthub.modules.features.chat.exception.MessageNotFoundException;
import com.connecthub.modules.features.chat.exception.ReplyToMessageNotFoundException;
import com.connecthub.modules.features.chat.mapper.MessageMapper;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.chat.repository.MessageMediaRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageMediaRepository messageMediaRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
    private final MessageMapper messageMapper;

    @Transactional
    public Message saveMessage(Conversation conversation, User user, SendMessageRequest request) {
        Message message = Message.builder()
                .id(AppUtil.generateUUID())
                .conversation(conversation)
                .sender(user)
                .content(request.getContent())
                .build();

        if (request.getReplyToMessageId() != null) {
            Message replyToMessage = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new ReplyToMessageNotFoundException(request.getReplyToMessageId().toString()));
            message.setReplyTo(replyToMessage);
        }

        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        messageRepository.save(message);

        if (hasMedia) {
            List<MessageMedia> mediaList = request.getMedia().stream()
                    .map(mediaRequest -> MessageMedia.builder()
                            .id(AppUtil.generateUUID())
                            .message(message)
                            .url(mediaRequest.getUrl())
                            .type(mediaRequest.getType())
                            .build())
                    .toList();
            messageMediaRepository.saveAll(mediaList);
        }

        // Không tạo MessageReceipt ở đây. SENT được suy ra từ việc Message
        // tồn tại; DELIVERED ghi đúng lúc push WS thành công (xem
        // DeliveryTrackingService.markDelivered), không phải lúc gửi.
        return message;
    }


    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public void deleteMessage(UUID messageId) {
        UUID currentUser = AppUtil.userIdFormAuthentication();
        User user = userRepository.getReferenceById(currentUser);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId.toString()));
        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setDeletedBy(user);
        messageRepository.save(message);
        webSocketService.pushMessageDeleted(messageId, message.getConversation().getId());
    }


    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public CursorResponse<MessageResponse> getMessages(UUID conversationId, UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        boolean isMember = conversationMemberRepository
                .existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) throw new ConversationAccessDeniedException();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);

        List<Message> messages = new ArrayList<>(
                messageRepository.findMessagesByConversationId(conversationId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                messages, size, Message::getId,
                msg -> messageMapper.toResponse(
                        msg, msg.getSender(), conversation,
                        MemberStatus.ACCEPTED, null
                )
        );
    }


}
