package com.connecthub.modules.features.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NotificationUnreadResponse {
    private Long unreadCount;
}
