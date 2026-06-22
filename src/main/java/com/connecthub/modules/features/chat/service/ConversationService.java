package com.connecthub.modules.features.chat.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.AcceptConversationRequest;
import com.connecthub.modules.features.chat.dto.response.ConversationSummaryResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
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

        // 2 query cho CẢ BATCH, không phải 1 query/conversation — tránh N+1
        Map<UUID, Message> lastMessageByConvId = findLastMessages(conversationIds);
        Map<UUID, Long> unreadCountByConvId = countUnreadBatch(conversationIds, currentUserId);

        return AppUtil.buildCursorResponse(
                conversations, size, Conversation::getId,
                conv -> toSummaryResponse(
                        conv,
                        currentUserId,
                        lastMessageByConvId.get(conv.getId()),
                        unreadCountByConvId.getOrDefault(conv.getId(), 0L)
                )
        );
    }

    private ConversationSummaryResponse toSummaryResponse(
            Conversation conv, UUID currentUserId, Message lastMessage, long unreadCount
    ) {
        // Chỉ tìm member 1 lần, dùng lại cho cả myStatus và (nếu PRIVATE) peer.
        ConversationMember myMember = getMyMember(conv, currentUserId);
        ConversationMember peerMember = conv.getType() == ConversationType.PRIVATE
                ? getPeerMember(conv, currentUserId)
                : null;

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
                    .lastMessageAt(lastMessage.getCreatedAt());
        }

        if (peerMember != null) {
            builder.peerId(peerMember.getUser().getId());
        }

        return builder.build();
    }

    // Nhận sẵn peerMember (null nếu GROUP) để không phải tự tìm lại trong stream.
    private String resolveDisplayName(Conversation conversation, ConversationMember peerMember) {
        if (conversation.getType() == ConversationType.PRIVATE) {
            return peerMember.getUser().getUsername();
        }

        if (conversation.getName() != null && !conversation.getName().isBlank()) {
            return conversation.getName();
        }

        List<String> otherUsernames = conversation.getConversationMembers().stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(AppUtil.userIdFormAuthentication()))
                .map(User::getUsername)
                .limit(3)
                .toList();

        int remaining = conversation.getConversationMembers().size() - 1 - otherUsernames.size();
        String base = String.join(", ", otherUsernames);
        return remaining > 0 ? base + " và " + remaining + " người khác" : base;
    }

    private String resolveDisplayAvatar(Conversation conv, ConversationMember peerMember) {
        if (conv.getType() == ConversationType.PRIVATE) {
            return peerMember.getUser().getAvatarUrl();
        }
        return conv.getAvatarUrl();
    }

    private ConversationMember getMyMember(Conversation conversation, UUID currentUserId) {
        return conversation.getConversationMembers().stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(SenderNotConversationMemberException::new);
    }

    // Chỉ dùng cho PRIVATE — group có nhiều "đối phương", không có 1 peer duy nhất.
    private ConversationMember getPeerMember(Conversation conversation, UUID currentUserId) {
        if (conversation.getType() != ConversationType.PRIVATE) {
            throw new InvalidTypeConversionException(ConversationType.PRIVATE);
        }
        return conversation.getConversationMembers().stream()
                .filter(member -> !member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(SenderNotConversationMemberException::new);
    }

    private Map<UUID, Message> findLastMessages(List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) return Map.of();
        return messageRepository.findLastMessagesForConversations(conversationIds).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m, (a, b) -> a));
    }

    private Map<UUID, Long> countUnreadBatch(List<UUID> conversationIds, UUID userId) {
        if (conversationIds.isEmpty()) return Map.of();
        return messageReceiptRepository
                .countUnreadGroupedByConversation(conversationIds, userId, MessageStatus.READ)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }
}
