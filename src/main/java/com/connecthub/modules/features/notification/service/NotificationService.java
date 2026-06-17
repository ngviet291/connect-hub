package com.connecthub.modules.features.notification.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.dto.response.NotificationUnreadResponse;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.exception.NotificationNotFoundException;
import com.connecthub.modules.features.notification.mapper.NotificationMapper;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void read(UUID id) {
        String username = AppUtil.usernameFromAuthentication();
        Notification notification = notificationRepository.findByIdAndRecipientUsername(id, username)
                .orElseThrow(NotificationNotFoundException::new);
        notification.setRead(true);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void readAll() {
        String username = AppUtil.usernameFromAuthentication();
        notificationRepository.markAsReadAllByIdAndRecipientUsername(username);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public NotificationUnreadResponse countUnread() {
        String username = AppUtil.usernameFromAuthentication();
        return NotificationUnreadResponse.builder()
                .unreadCount(notificationRepository.countUnreadByRecipientUsername(username))
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<NotificationResponse> getNotification(UUID cursor, int size) {
        String username = AppUtil.usernameFromAuthentication();

        // Lấy dư 1 phần tử (size + 1) bằng Limit (Spring Data 3.x)
        // Bọc vào ArrayList để tránh lỗi UnsupportedOperationException khi remove
        List<Notification> notifications = new ArrayList<>(
                notificationRepository.findByRecipientUsername(username, cursor, Limit.of(size + 1))
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
}
