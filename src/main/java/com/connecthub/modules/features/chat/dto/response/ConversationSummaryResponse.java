package com.connecthub.modules.features.chat.dto.response;

import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {
    private UUID conversationId;
    private ConversationType type;          // PRIVATE | GROUP

    // Tên/avatar hiển thị — đã resolve sẵn theo đúng nguyên tắc:
    // PRIVATE → username/avatar HIỆN TẠI của đối phương
    // GROUP   → tên/avatar riêng của nhóm, hoặc tự sinh từ thành viên hiện tại
    private String displayName;
    private String displayAvatarUrl;

    // Trạng thái CỦA CHÍNH MÌNH trong conversation này (không phải của đối phương)
    private MemberStatus myStatus;           // ACCEPTED | PENDING | BLOCKED

    // Preview tin nhắn cuối — đây snapshot lúc gửi, đúng nguyên tắc lịch sử
    private UUID lastMessageId;
    private String lastMessageContent;
    private String lastMessageSenderUsername;
    private LocalDateTime lastMessageAt;

    private long unreadCount;

    // Chỉ có ý nghĩa với PRIVATE — id của đối phương để FE điều hướng/thao tác
    // (follow, block...). Null với GROUP.
    private UUID peerId;
}