package com.connecthub.modules.features.chat.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.AcceptConversationRequest;
import com.connecthub.modules.features.chat.dto.response.ConversationSummaryResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.exception.ConversationMemberNotFoundException;
import com.connecthub.modules.features.chat.exception.ConversationNotFoundException;
import com.connecthub.modules.features.chat.exception.InvalidTypeConversionException;
import com.connecthub.modules.features.chat.exception.SenderNotConversationMemberException;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.chat.repository.MessageReceiptRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;

    @Transactional
    public Conversation createPrivateConversation(
            User sender,
            User recipient,
            MemberStatus recipientStatus
    ) {

        Conversation conversation = Conversation.builder()
                .id(AppUtil.generateUUID())
                .type(ConversationType.PRIVATE)
                .build();

        ConversationMember senderMember = ConversationMember.builder()
                .conversation(conversation)
                .user(sender)
                .build();

        ConversationMember recipientMember = ConversationMember.builder()
                .conversation(conversation)
                .user(recipient)
                .status(recipientStatus)
                .build();

        conversation.setConversationMembers(
                new HashSet<>(List.of(senderMember, recipientMember))
        );

        return conversationRepository.save(conversation);
    }

    @Transactional
    public void acceptConversationMember(AcceptConversationRequest request) {
        ConversationMember member = conversationMemberRepository
                .findConversationMemberByConversationIdAndUserId(
                        request.getConversationId(),
                        request.getUserAccept()
                )
                .orElseThrow(ConversationMemberNotFoundException::new);

        member.setStatus(MemberStatus.ACCEPTED);
        conversationMemberRepository.save(member);
    }

    public CursorResponse<ConversationSummaryResponse> getConversations(UUID cursor, int size, MemberStatus status) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        List<Conversation> conversations = new ArrayList<>(
                conversationRepository.findConversationsByUserId(currentUserId, cursor, Limit.of(size + 1), status)
        );

        List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, Message> lastMessageByConvId = findLastMessages(conversationIds);

        return AppUtil.buildCursorResponse(
                conversations, size, Conversation::getId,
                conv -> toSummaryResponse(conv, currentUserId, lastMessageByConvId.get(conv.getId()))
        );
    }

    private ConversationSummaryResponse toSummaryResponse(
            Conversation conv, UUID currentUserId, Message lastMessage
    ) {
        ConversationMember myMember = getMyMember(conv, currentUserId);
        ConversationMember peerMember = conv.getType() == ConversationType.PRIVATE
                ? getPeerMember(conv, currentUserId)
                : null;

        UUID lastReadId = myMember.getLastReadMessage() != null
                ? myMember.getLastReadMessage().getId()
                : null;
        long unreadCount = messageRepository.countUnreadSinceLastRead(conv.getId(), currentUserId, lastReadId);

        ConversationSummaryResponse.ConversationSummaryResponseBuilder builder =
                ConversationSummaryResponse.builder()
                        .conversationId(conv.getId())
                        .type(conv.getType())
                        .displayName(resolveDisplayName(conv, peerMember))
                        .displayAvatarUrl(resolveDisplayAvatar(conv, peerMember))
                        .myStatus(myMember.getStatus())
                        .unreadCount(unreadCount);

        if (lastMessage != null) {
            builder.lastMessageId(lastMessage.getId())
                    .lastMessageContent(lastMessage.getContent())
                    .lastMessageSenderUsername(lastMessage.getSender().getUsername())
                    .lastMessageAt(lastMessage.getCreatedAt())
                    .lastMessageStatus(resolveLastMessageStatus(conv, lastMessage, lastReadId));
        }

        if (peerMember != null) {
            builder.peerId(peerMember.getUser().getId());
        }

        return builder.build();
    }

    // PRIVATE: nếu mình đã đọc tới >= tin cuối → READ. Nếu chưa, tra receipt
// để biết SENT/DELIVERED. GROUP: chỉ SENT/DELIVERED dựa vào deliveredAt,
// không có READ theo từng người.
    private MessageStatus resolveLastMessageStatus(Conversation conv, Message lastMessage, UUID lastReadId) {
        if (conv.getType() == ConversationType.PRIVATE) {
            if (lastReadId != null && lastMessage.getId().compareTo(lastReadId) <= 0) {
                return MessageStatus.READ;
            }
            return messageReceiptRepository
                    .findByMessageIdAndUserId(lastMessage.getId(), getOtherPartyId(conv, lastMessage))
                    .map(MessageReceipt::getStatus)
                    .orElse(MessageStatus.SENT);
        }
        return lastMessage.getDeliveredAt() != null ? MessageStatus.DELIVERED : MessageStatus.SENT;
    }

    private UUID getOtherPartyId(Conversation conv, Message lastMessage) {
        // Trạng thái hiển thị nên là góc nhìn "đối phương đã nhận tin của tôi
        // chưa", áp dụng khi chính mình là sender của lastMessage.
        return getPeerMember(conv, lastMessage.getSender().getId()).getUser().getId();
    }

    private Map<UUID, Message> findLastMessages(List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) return Map.of();
        return messageRepository.findLastMessagesForConversations(conversationIds).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m, (a, b) -> a));
    }

    private ConversationMember getMyMember(Conversation conversation, UUID currentUserId) {
        return conversation.getConversationMembers().stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(SenderNotConversationMemberException::new);
    }

    private ConversationMember getPeerMember(Conversation conversation, UUID currentUserId) {
        if (conversation.getType() != ConversationType.PRIVATE) {
            throw new InvalidTypeConversionException(ConversationType.PRIVATE);
        }
        return conversation.getConversationMembers().stream()
                .filter(member -> !member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(SenderNotConversationMemberException::new);
    }

    private String resolveDisplayName(Conversation conversation, ConversationMember peerMember) {
        String format =  "%s và %d người khác";
        if (conversation.getType() == ConversationType.PRIVATE) {
            return peerMember.getUser().getUsername();
        }
        if (conversation.getName() != null && !conversation.getName().isBlank()) {
            return conversation.getName();
        }
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        List<String> otherUsernames = conversation.getConversationMembers().stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .map(User::getUsername)
                .limit(3)
                .toList();
        int remaining = conversation.getConversationMembers().size() - 1 - otherUsernames.size();
        String base = String.join(", ", otherUsernames);
        return remaining > 0 ? String.format(format, base, remaining) : base;
    }

    private String resolveDisplayAvatar(Conversation conv, ConversationMember peerMember) {
        if (conv.getType() == ConversationType.PRIVATE) {
            return peerMember.getUser().getAvatarUrl();
        }
        return conv.getAvatarUrl();
    }
}
