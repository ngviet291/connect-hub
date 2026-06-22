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
import com.connecthub.modules.features.chat.exception.ConversationMemberNotFoundException;
import com.connecthub.modules.features.chat.exception.ConversationNotFoundException;
import com.connecthub.modules.features.chat.exception.InvalidTypeConversionException;
import com.connecthub.modules.features.chat.exception.SenderNotConversationMemberException;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

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

    public String resolveDisplayName(Conversation conversation, UUID currentUserId) {
        if (conversation.getType() == ConversationType.PRIVATE) {
            return getPeerMember(conversation, currentUserId).getUser().getUsername();
        }

        if (conversation.getName() != null && !conversation.getName().isBlank()) {
            return conversation.getName();
        }

        List<String> otherUsernames = conversation.getConversationMembers().stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .map(User::getUsername)
                .limit(3)
                .toList();

        int remaining = conversation.getConversationMembers().size() - 1 - otherUsernames.size();
        String base = String.join(", ", otherUsernames);
        return remaining > 0 ? base + " và " + remaining + " người khác" : base;
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


    public CursorResponse<ConversationSummaryResponse> getConversations(UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        List<Conversation> conversations = new ArrayList<>(
                conversationRepository.findConversationsByUserId(currentUserId, cursor, Limit.of(size + 1))
        );
        return AppUtil.buildCursorResponse(conversations, size, Conversation::getId, conv -> toSummaryResponse(conv, currentUserId));
    }
    private ConversationSummaryResponse toSummaryResponse(Conversation conv, UUID currentUserId) {
        ConversationMember myMember = getMyMember(conv, currentUserId);
//        Message lastMessage = /findLastMessage(conv); // helper đã có sẵn theo bạn nói trước đó

        ConversationSummaryResponse.ConversationSummaryResponseBuilder builder =
                ConversationSummaryResponse.builder()
                        .conversationId(conv.getId())
                        .type(conv.getType())
                        .displayName(resolveDisplayName(conv, currentUserId))
//                        .displayAvatarUrl(resolveDisplayAvatar(conv, currentUserId))
                        .myStatus(myMember.getStatus());
//                        .unreadCount(countUnread(conv, currentUserId));

//        if (lastMessage != null) {
//            builder.lastMessageId(lastMessage.getId())
//                    .lastMessageContent(lastMessage.getContent())
//                    .lastMessageSenderUsername(lastMessage.getSender().getUsername())
//                    .lastMessageAt(lastMessage.getCreatedAt())
//                    .lastMessageStatus(lastMessage.getStatus());
//        }

        if (conv.getType() == ConversationType.PRIVATE) {
            builder.peerId(getPeerMember(conv, currentUserId).getUser().getId());
        }

        return builder.build();
    }

    private ConversationMember getMyMember(Conversation conversation, UUID currentUserId) {
        return conversation.getConversationMembers().stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(SenderNotConversationMemberException::new);
    }
}
