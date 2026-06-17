package com.connecthub.modules.features.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String username;
    private String avatarUrl;
}
