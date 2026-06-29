package com.connecthub.modules.features.notification.dto.request;

import com.connecthub.modules.features.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = """
        Request payload for creating a notification.
        If the notification is related to a post, provide the post ID and target URL.
        If it's related to a conversation, provide the conversation ID.
        The recipient is the user who will receive the notification,
         and the actor is the user who triggered the notification.
        """)

/**
 *  Chỉ có service được tạo ra  notification, không có user nào được tạo ra notification
 *  Notification được tạo ra khi có một hành động nào đó xảy ra, ví dụ:
 *  - User A like post của User B => Notification được tạo ra cho User B với  actor là User A, type là LIKE, postId là ID của post
 *  - User A comment post của User B => Notification được tạo ra cho User B với actor là User A, type là COMMENT, postId là ID của post
 *  - User A follow User B => Notification được tạo ra cho User B với actor là User A, type là FOLLOW, postId là null
 */
public class NotificationRequest {
    @Schema(description = "The recipient of the notification. This is the user who will receive the notification.")
    private UUID recipient;
    @Schema(description = "The actor who triggered the notification. For example, if a user liked a post, the actor would be the user who liked it.")
    private UUID actor;
    @Schema(description = "If the notification is related to a post, provide the post ID. Otherwise, it can be null.")
    private UUID postId;
    @Schema(description = "If the notification is related to a conversation, provide the conversation ID. Otherwise, it can be null.")
    private UUID conversationId;
    private String content;
    @Schema(description = "The URL that the notification should link to. For example, if the notification is about a new message, this could be the URL to the conversation.")
    private String targetUrl;
    @Schema(description = "The type of notification. This could be an enum representing different types of notifications, such as 'LIKE', 'COMMENT', 'FOLLOW', etc.")
    @NotNull
    private NotificationType type;
    private boolean isRead;
}
