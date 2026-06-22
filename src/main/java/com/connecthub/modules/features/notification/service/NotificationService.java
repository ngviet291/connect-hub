package com.connecthub.modules.features.notification.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.exception.ConversationNotFoundException;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.notification.dto.request.NotificationRequest;
import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.dto.response.NotificationUnreadResponse;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.exception.NotificationNotFoundException;
import com.connecthub.modules.features.notification.mapper.NotificationMapper;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final PostRepository postRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void read(UUID id) {
        UUID userId = AppUtil.userIdFormAuthentication();
        Notification notification = notificationRepository.findByIdAndRecipientId(id, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.setRead(true);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void readAll() {
        UUID userId = AppUtil.userIdFormAuthentication();
        notificationRepository.markAsReadAllByIdAndRecipientId(userId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public NotificationUnreadResponse countUnread() {
        UUID userId = AppUtil.userIdFormAuthentication();
        return NotificationUnreadResponse.builder()
                .unreadCount(notificationRepository.countUnreadByRecipientIdAndIsReadFalse(userId))
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<NotificationResponse> getNotification(UUID cursor, int size, NotificationType type) {
        UUID userId = AppUtil.userIdFormAuthentication();
        // Lấy dư 1 phần tử (size + 1) bằng Limit (Spring Data 3.x)
        // Bọc vào ArrayList để tránh lỗi UnsupportedOperationException khi remove
        List<Notification> notifications = new ArrayList<>(
                notificationRepository.findByRecipientIdAndType(userId, cursor, Limit.of(size + 1), type)
        );

        boolean hasNext = notifications.size() > size;

        if (hasNext) {
            notifications.removeLast(); // Xóa phần tử dư thừa
        }

        // Lấy ID của phần tử cuối cùng làm cursor tiếp theo
        String nextCursor = notifications.isEmpty()
                ? null
                : notifications.getLast().getId().toString();

        return CursorResponse.<NotificationResponse>builder()
                .content(
                        notifications.stream()
                                .map(notificationMapper::toNotificationResponse)
                                .toList()
                )
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }


    /**
     * Chỉ cho phép các service khác gọi, không cho phép người dùng trực tiếp gọi API này
     * @param request
     * @return
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        System.out.println("Creating notification: " + request);
        //
        User recipient = userRepository.getReferenceById(request.getRecipient());

        User actor = userRepository.getReferenceById(request.getActor());
        Notification.NotificationBuilder builder = Notification.builder()
                .recipient(recipient)
                .id(AppUtil.generateUUID())
                .actor(actor)
                .type(request.getType())
                .targetUrl(request.getTargetUrl());

        switch (request.getType()) {

            case MESSAGE -> {
                if (request.getConversationId() == null) {
                    throw new ConversationNotFoundException();
                }
                Conversation conversation = conversationRepository.getReferenceById(request.getConversationId());

                builder.conversation(conversation);
            }

            case LIKE, COMMENT, REACTION, MENTION, REPOST -> {
                if (request.getPostId() == null) {
                    throw new PostNotFoundException();
                }
                Post post = postRepository.getReferenceById(request.getPostId());
                builder.post(post);
            }

            case FOLLOW, SYSTEM, MESSAGE_PENDING -> {
                // không cần xử lý thêm
            }

            default -> throw new UnsupportedOperationException(
                    "Notification type not supported: " + request.getType()
            );
        }

        Notification notification =
                notificationRepository.save(builder.build());

        return notificationMapper.toNotificationResponse(notification);
    }

}
