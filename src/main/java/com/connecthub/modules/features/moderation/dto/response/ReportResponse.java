package com.connecthub.modules.features.moderation.dto.response;

import com.connecthub.modules.features.moderation.enums.ReasonType;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import com.connecthub.modules.features.post.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {
 * "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
 * "reporterId": "11111111-1111-1111-1111-111111111111",
 * "targetUserId": "b1e2c3d4-0000-4a11-9a11-abc123456789",
 * "postId": null,
 * "reason": "HARASSMENT",
 * "description": "Người này liên tục gửi tin nhắn đe dọa tôi.",
 * "status": "PENDING",
 * "createdAt": "2026-06-25T10:15:30"
 * }
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReportResponse {
    private String reportId;
    private String reporterId;
    private String targetUserId;
    private String postId;
    private ReasonType reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
