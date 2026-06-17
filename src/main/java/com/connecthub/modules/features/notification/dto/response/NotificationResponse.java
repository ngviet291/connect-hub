package com.connecthub.modules.features.notification.dto.response;

import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String content;
    private String targetUrl;
    private NotificationType type;
    private PostSummaryResponse post;
    private UserSummaryResponse user;
    private boolean isRead;
    private LocalDateTime createdAt;
}
