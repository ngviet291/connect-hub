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
    public CursorResponse<NotificationResponse> getNotification(UUID cursor, int size) {
        UUID userId = AppUtil.userIdFormAuthentication();
        // Lấy dư 1 phần tử (size + 1) bằng Limit (Spring Data 3.x)
        // Bọc vào ArrayList để tránh lỗi UnsupportedOperationException khi remove
        List<Notification> notifications = new ArrayList<>(
                notificationRepository.findByRecipientId(userId, cursor, Limit.of(size + 1))
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
