package com.connecthub.modules.features.chat.dto.response;

import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MessageResponse {
    private UUID messageId;
    private String content;
    private LocalDateTime sentAt;
    private MessageStatus status;        // SENT | DELIVERED | READ | DELETED

    // ── Thông tin conversation ──────────────────────
    private UUID conversationId;
    private MemberStatus conversationStatus; // ACCEPTED | PENDING | BLOCKED

    // ── Thông tin người gửi ─────────────────────────
    private UUID senderId;
    private String senderUsername;
    private String senderAvatarUrl;

    // ── Media đính kèm (nếu có) ─────────────────────
    private List<MediaResponse> media;

}
