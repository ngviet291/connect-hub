package com.connecthub.modules.features.chat.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.AcceptConversationRequest;
import com.connecthub.modules.features.chat.dto.request.CreateGroupConversationRequest;
import com.connecthub.modules.features.chat.dto.response.ConversationDetailResponse;
import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import com.connecthub.modules.features.chat.dto.response.ConversationSummaryResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberRole;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.exception.*;
import com.connecthub.modules.features.chat.mapper.ConversationMapper;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.chat.repository.MessageReceiptRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final ConversationMapper conversationMapper;
    private final UserRepository userRepository;

    @Value("${app.chat.group.max-members:100}")
    private String MAX_GROUP_MEMBERS;

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
                .status(MemberStatus.ACCEPTED) // người tạo convo private tự động là ACCEPTED
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
        String format = "%s và %d người khác";
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


    public ConversationDetailResponse getConversationDetail(UUID id) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        Conversation conversation = conversationRepository.findByIdWithMembers(id)
                .orElseThrow(ConversationNotFoundException::new);

        // Member check: 403 nếu không phải thành viên — không dùng try/catch
        // exception khác (SenderNotConversationMemberException) ở đây vì ngữ
        // nghĩa khác nhau: đó là lỗi nội bộ (logic sai), còn đây là access
        // control hợp lệ cần trả đúng mã 403 cho client.
        boolean isMember = conversation.getConversationMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(currentUserId));
        if (!isMember) {
            throw new ConversationAccessDeniedException();
        }

        ConversationMember myMember = getMyMember(conversation, currentUserId);
        ConversationMember peerMember = conversation.getType() == ConversationType.PRIVATE
                ? getPeerMember(conversation, currentUserId)
                : null;

        List<ConversationMemberResponse> members = conversation.getConversationMembers().stream()
                .map(conversationMapper::toMemberResponse)
                .toList();

        return ConversationDetailResponse.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType())
                .displayName(resolveDisplayName(conversation, peerMember))
                .displayAvatarUrl(resolveDisplayAvatar(conversation, peerMember))
                .myStatus(myMember.getStatus())
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .build();
    }


    @Transactional
    public ConversationSummaryResponse createGroupConversation(CreateGroupConversationRequest request) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        String groupName = request.getName();

        if (groupName != null) {
            groupName = groupName.trim();
        }
        User adminUser = userRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        // Dedupe + loại admin khỏi danh sách members nếu client lỡ gửi kèm
        // chính họ — admin được thêm riêng với role khác. Copy ra collection
        // mới, không mutate DTO của client.
        Set<UUID> otherMemberIds = request.getMembers() == null
                ? Set.of()
                : new LinkedHashSet<>(request.getMembers());
        otherMemberIds.remove(currentUserId);

        if (otherMemberIds.isEmpty()) {
            throw new InvalidChatRequestException(); // group cần ít nhất 1 thành viên khác admin
        }
        if (otherMemberIds.size() > Integer.parseInt(MAX_GROUP_MEMBERS)) {
            throw new GroupMemberLimitExceededException(MAX_GROUP_MEMBERS);
        }

        List<User> otherUsers = userRepository.findAllById(otherMemberIds);
        if (otherUsers.size() != otherMemberIds.size()) {
            // findAllById âm thầm bỏ qua ID không tồn tại — phải tự kiểm tra
            // để không tạo group thiếu người mà không ai biết.
            throw new UserNotFoundException();
        }

        // nếu admin không đặt tên group thì tự động
        // tạo tên mặc định theo format "A, B và X người khác" (tương tự cách hiển thị),
        // với A là admin và B,C... là các thành viên đầu tiên trong list.
        // Cách này giúp tránh tình trạng group có tên trống hoặc toàn dấu cách,
        // đồng thời vẫn có tên hiển thị ý nghĩa ngay cả khi admin quên đặt tên.

        if (groupName == null || groupName.isBlank()) {
            groupName = generateDefaultGroupName(adminUser, otherUsers);
        }
        Conversation conversation = Conversation.builder()
                .id(AppUtil.generateUUID())
                .type(ConversationType.GROUP)
                .name(groupName)
                .build();

        Set<ConversationMember> conversationMembers = new HashSet<>();

        conversationMembers.add(ConversationMember.builder()
                .conversation(conversation)
                .user(adminUser)
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build());

        otherUsers.forEach(member -> conversationMembers.add(
                ConversationMember.builder()
                        .conversation(conversation)
                        .user(member)
                        .role(MemberRole.MEMBER)
                        .status(MemberStatus.ACCEPTED)
                        .joinedAt(LocalDateTime.now())
                        .build()
        ));

        conversation.setConversationMembers(conversationMembers);
        conversationRepository.save(conversation);

        return toSummaryResponse(conversation, currentUserId, null);
    }


    private String generateDefaultGroupName(
            User admin,
            List<User> members
    ) {
        return Stream.concat(
                        Stream.of(admin),
                        members.stream()
                )
                .map(User::getUsername)
                .limit(3)
                .collect(Collectors.joining(", "));
    }
}
