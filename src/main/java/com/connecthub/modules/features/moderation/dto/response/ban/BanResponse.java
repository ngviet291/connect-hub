package com.connecthub.modules.features.moderation.dto.response.ban;

import com.connecthub.modules.features.moderation.enums.BanReason;
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

public class BanResponse {
    private UUID id;
    private UUID userId;
    private UUID bannedId;
    private BanReason reason;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    private boolean active;           // tính từ isActive()

    // unban info — null nếu chưa unban
    private UUID unbannedById;
    private String unbannedByUsername;
    private LocalDateTime unbannedAt;
    private String unbanReason;
}
