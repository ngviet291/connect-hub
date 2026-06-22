package com.connecthub.modules.features.notification.event;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.modules.features.notification.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.notification.enums.NotificationType;
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
public class NotificationEvent implements DomainEvent {
    private UUID recipientId;
    private NotificationType type;
    private String content;
    private UserSummaryResponse actor;
    private UUID entityId;
    private LocalDateTime createdAt;
}
