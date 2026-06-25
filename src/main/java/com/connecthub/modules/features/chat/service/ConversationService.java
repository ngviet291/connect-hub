package com.connecthub.modules.features.chat.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.common.service.WebSocketService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.dto.request.UpdateConversationRequest;
import com.connecthub.modules.features.chat.dto.request.AcceptConversationRequest;
import com.connecthub.modules.features.chat.dto.request.AddMembersRequest;
import com.connecthub.modules.features.chat.dto.request.CreateGroupConversationRequest;
import com.connecthub.modules.features.chat.dto.request.UpdateMemberRoleRequest;
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
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final MediaStorageService mediaStorageService;
    private final WebSocketService webSocketService;

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

    @PreAuthorize("hasRole('ROLE_USER')")
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

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
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

    //=========================================HELPERS FOR CONVERSATION SUMMARY RESPONSE==================================================//
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
//=========================================END=====================================================================//


    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(UUID id, UUID membersCursor, int membersSize) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(ConversationNotFoundException::new);

        ConversationMember myMember = conversationMemberRepository
                .findConversationMemberByConversationIdAndUserId(id, currentUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACCEPTED || m.getStatus() == MemberStatus.PENDING)
                .orElseThrow(ConversationAccessDeniedException::new);

        ConversationMember peerMember = conversation.getType() == ConversationType.PRIVATE
                ? conversationMemberRepository
                .findConversationMemberByConversationIdAndUserIdNot(id, currentUserId)
                .orElse(null)
                : null;

        // size + 1 để helper tự xác định hasNext
        Limit limit = Limit.of(membersSize + 1);
        List<ConversationMember> rawMembers = new ArrayList<>(
                conversationMemberRepository.findMembersByConversationId(id, membersCursor, limit)
        );

        CursorResponse<ConversationMemberResponse> membersPage = AppUtil.buildCursorResponse(
                rawMembers,
                membersSize,
                m -> m.getUser().getId(),
                conversationMapper::toMemberResponse
        );

        return ConversationDetailResponse.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType())
                .displayName(resolveDisplayName(conversation, peerMember))
                .displayAvatarUrl(resolveDisplayAvatar(conversation, peerMember))
                .myStatus(myMember.getStatus())
                .members(membersPage)
                .createdAt(conversation.getCreatedAt())
                .build();
    }


    @PreAuthorize("hasRole('ROLE_USER')")
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


    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public void leaveConversation(UUID conversationId) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        ConversationMember conversationMember = conversationMemberRepository.findConversationMemberByConversationIdAndUserId(
                        conversationId, currentUserId)
                .orElseThrow(ConversationMemberNotFoundException::new);

        if (conversationMember.getRole() == MemberRole.ADMIN) {
            throw new AdminCannotLeaveGroupException();
        }

        conversationMember.setStatus(MemberStatus.LEFT);
        conversationMember.setLeftAt(LocalDateTime.now());
        conversationMemberRepository.save(conversationMember);
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public void removeMember(UUID conversationId, UUID memberId) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        requireAdmin(conversationId, currentUserId);

        if (memberId.equals(currentUserId)) {
            throw new InvalidChatRequestException();
        }

        ConversationMember targetMember = conversationMemberRepository
                .findConversationMemberByConversationIdAndUserId(
                        conversationId, memberId)
                .orElseThrow(ConversationMemberNotFoundException::new);

        if (targetMember.getRole() == MemberRole.ADMIN) {
            throw new CannotRemoveAdminException();
        }

        targetMember.setStatus(MemberStatus.REMOVED);
        targetMember.setLeftAt(LocalDateTime.now());
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public ConversationSummaryResponse updateConversation(UUID conversationId, UpdateConversationRequest request) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        ConversationMember currentMember = requireAdmin(conversationId, currentUserId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);

        conversation.setName(request.getName() != null ? request.getName().trim() : conversation.getName());
        if (request.getAvatar() != null) {
            try {
                // upload new image first
                UploadMediaResponse uploadResponse = mediaStorageService
                        .uploadImage(request.getAvatar().getBytes(), "conversation-avatar" + conversationId)
                        .join();

                // delete old avatar in storage if exists (best-effort)
                if (conversation.getPublicId() != null) {
                    try {
                        mediaStorageService.delete(conversation.getPublicId());
                    } catch (Exception ex) {
                        throw new UploadMediaException();
                    }
                }

                conversation.setAvatarUrl(uploadResponse.getUrl());
                conversation.setPublicId(uploadResponse.getPublicId());

                Conversation saved = conversationRepository.save(conversation);
                return toSummaryResponse(saved, currentUserId, null);
            } catch (Exception e) {
                // If the underlying cause is an IO problem, wrap and rethrow as UploadMediaException
                if (e instanceof UploadMediaException || (e.getCause() != null && e.getCause() instanceof UploadMediaException)) {
                    throw new UploadMediaException();
                }
                throw new UploadMediaException();
            }
        }
        return toSummaryResponse(conversation, currentUserId, null);
    }


    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public ConversationDetailResponse addMembers(UUID conversationId, AddMembersRequest request) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        requireAdmin(conversationId, currentUserId);

        Set<UUID> requestedIds = new LinkedHashSet<>(request.getMemberIds());

        // Validate user tồn tại trước (UPSERT không tự báo lỗi "user not found")
        List<User> users = userRepository.findAllById(requestedIds);
        if (users.size() != requestedIds.size()) {
            throw new UserNotFoundException();
        }

        long currentActiveCount = conversationMemberRepository
                .countByConversationIdAndStatusIn(conversationId,
                        List.of(MemberStatus.ACCEPTED, MemberStatus.PENDING));
        int maxMembers = Integer.parseInt(MAX_GROUP_MEMBERS);
        if (currentActiveCount + requestedIds.size() > maxMembers) {
            throw new GroupMemberLimitExceededException(MAX_GROUP_MEMBERS);
        }

        LocalDateTime now = LocalDateTime.now();
        int totalAffected = 0;
        for (UUID userId : requestedIds) {
            totalAffected += conversationMemberRepository.upsertMember(conversationId, userId, now);
        }

        if (totalAffected == 0) {
            // mọi id đều đã ACCEPTED/PENDING từ trước — không có gì để add
            throw new InvalidChatRequestException();
        }

        List<ConversationMember> affectedMembers = conversationMemberRepository
                .findAllByConversationIdAndUserIdIn(conversationId, requestedIds);

        webSocketService.pushAddNewMembers(conversationId,
                affectedMembers.stream().map(conversationMapper::toMemberResponse).toList());

        return getConversationDetail(conversationId, null, 20);
    }


    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public ConversationDetailResponse updateMemberRole(UUID conversationId, UUID memberId, UpdateMemberRoleRequest request) {

        requireAdmin(conversationId, AppUtil.userIdFormAuthentication());

        ConversationMember targetMember = conversationMemberRepository
                .findConversationMemberByConversationIdAndUserId(conversationId, memberId)
                .orElseThrow(ConversationMemberNotFoundException::new);
        targetMember.setRole(request.getRole());
        conversationMemberRepository.save(targetMember);
        webSocketService.pushUpdateMemberRole(conversationId, conversationMapper.toMemberResponse(targetMember));
        return getConversationDetail(conversationId, null, 20);
    }


    /**
     * Kiểm tra xem userId có phải là admin của conversationId hay không.
     *
     * @param conversationId
     * @param userId
     * @return ConversationMember nếu là admin
     * @throws ConversationMemberNotFoundException   nếu không tìm thấy member
     * @throws ConversationPermissionDeniedException nếu không phải admin
     * @throws InvalidTypeConversionException        nếu conversation không phải là GROUP
     */
    private ConversationMember requireAdmin(UUID conversationId, UUID userId) {
        ConversationMember member = conversationMemberRepository
                .findConversationMemberByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationMemberNotFoundException::new);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new ConversationPermissionDeniedException();
        }
        if (member.getConversation().getType() != ConversationType.GROUP) {
            throw new InvalidTypeConversionException(ConversationType.GROUP);
        }
        return member;
    }
}
